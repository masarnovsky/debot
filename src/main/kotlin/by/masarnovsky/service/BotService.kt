package by.masarnovsky.service

import by.masarnovsky.*
import by.masarnovsky.db.*
import by.masarnovsky.util.*
import mu.KotlinLogging
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal

private val logger = KotlinLogging.logger {}

fun sendListOfDebtors(chatId: Long) {
    logger.info { "call sendListOfDebtors method for $chatId" }

    val map = findDebtorsWithLogs(chatId)
    connection()
    val currency = transaction {
        return@transaction findUserByChatId(chatId)!!.defaultCurrency
    }
    val text = constructListOfAllDebtors(map, currency)
    sendMessage(chatId, text)
}

fun newDebt(chatId: Long, command: String) {
    logger.info { "call newDebt method for $chatId" }
    val match = NEW_DEBTOR_PATTERN.toRegex().find(command)!!
    val (name, amount, comment) = match.destructured
    val (debtor, _) = newLogRecord(name, amount.toBigDecimal(), comment, chatId)

    connection()
    val text = transaction {
        val logs = findLogsForDebtorByDebtorId(debtor.id!!)
        val user = findUserByChatId(chatId)!!
        return@transaction formatNewLogRecord(debtor, user.defaultCurrency, logs)
    }

    sendMessage(chatId, text)
}

fun repay(chatId: Long, command: String) {
    logger.info { "call repay method for $chatId" }
    val match = Regex(REPAY_PATTERN).find(command)!!
    val (name, amount) = match.destructured
    try {
        val (debtor, log) = newLogRecord(name, amount.toBigDecimal(), REPAY_VALUE, chatId)

        connection()
        val text = transaction {
            val logs = findLogsForDebtorByDebtorId(debtor.id!!)
            val user = findUserByChatId(chatId)!!
            return@transaction formatRepayRecord(debtor, log, logs, user.defaultCurrency)
        }

        sendMessage(chatId, text)
    } catch (ex: NegativeBalanceException) {
        sendMessage(chatId, NEGATIVE_BALANCE_ERROR)
    }
}

fun deleteAllDebtsNoOption(chatId: Long, messageId: Int) {
    logger.info { "call deleteAllDebtsNoOption for $chatId" }
    editMessageTextAndInlineKeyboard(chatId, messageId, NOT_DELETE_HISTORY)
}

fun returnListOfMemesForInlineQuery(chatId: Long, queryId: String) {
    logger.info { "call returnListOfMemesForInlineQuery for $chatId with queryId=$queryId" }

    connection()
    val images = transaction {
        return@transaction findAllImages()
    }
    val queries = images.map { image -> createInlineQueryResultPhoto(image.url) }
    bot.answerInlineQuery(queryId, queries)
}

fun returnListOfDebtorsForInlineQuery(chatId: Long, queryId: String) {
    logger.info { "call returnListOfDebtorsForInlineQuery for $chatId with queryId=$queryId" }

    val debtors = findDebtorsWithLogs(chatId)

    connection()
    val currency = transaction {
        return@transaction findUserByChatId(chatId)!!.defaultCurrency
    }
    val queries = debtors.keys.map { debtor -> createInlineQueryResultArticle(debtor, debtors[debtor]!!, currency) }
    bot.answerInlineQuery(queryId, queries)
}

fun deleteAllDebts(chatId: Long, messageId: Int) {
    logger.info { "call deleteAllDebts for $chatId" }

    connection()

    val count = transaction {
        return@transaction deleteAllDebtorsForUser(chatId)
    }

    editMessageTextAndInlineKeyboard(chatId, messageId, constructDeleteDebtorsMessageBasedOnDeletedCount(count))
}

fun showDebtorLogs(chatId: Long, name: String) {
    logger.info { "call showDebtorLogs for $chatId" }
    connection()

    val (debtor, logs) = transaction {
        val debtor = findDebtorByUserIdAndName(chatId, name)
        val logs = if (debtor != null) findLogsForDebtorByDebtorId(debtor.id!!) else listOf()

        return@transaction Pair(debtor, logs)
    }

    if (debtor != null) {
        val currency = transaction {
            return@transaction findUserByChatId(chatId)!!.defaultCurrency
        }
        val header = formatDebtorHistoryHeader(debtor, currency)
        val footer = formatDebtorHistoricalAmount(debtor, logs, currency)
        val text = logs
            .reversed()
            .fold(header) { temp, log -> temp + log.summarize() }
            .plus(footer)
        sendMessage(chatId, text)
    } else {
        sendMessage(chatId, DEBTOR_NOT_FOUND)
    }
}

fun mergeDebtorsById(sourceUser: Debtor, destinationUser: Debtor): Int {
    logger.info { "call mergeDebtorsById($sourceUser, $destinationUser)" }
    connection()
    val sourceLogs = transaction {
        return@transaction findLogsForDebtorByDebtorId(sourceUser.id!!)
    }
    return sourceLogs
        .map { sourceLog ->
            insertNewLogAndRecalculateDebt(
                destinationUser,
                sourceLog.getAmountAsRawValue(),
                sourceLog.comment
            )
        }
        .count()
}

fun sendMergedDebtorCallback(chatId: Long, text: String) {
    val match = Regex(SHOW_MERGED_PATTERN).find(text)!!
    val (name) = match.destructured
    showDebtorLogs(chatId, name)
}

fun processRevertLastDebtorLog(chatId: Long, messageId: Int, text: String) {
    val match = Regex(REVERT_LAST_DEBTOR_LOG_PATTERN).find(text)!!
    val (debtorId, logId) = match.destructured

    connection()

    transaction {
        val debtor = findDebtorByUserIdAndId(chatId, debtorId.toLong())
        val log = findLogByIdAndDebtorId(logId.toLong(), debtorId.toLong())
        if (debtor != null && log != null) {
            debtor.totalAmount -= log.getAmountAsRawValue()

            if (debtor.totalAmount < BigDecimal.ZERO) {
                logger.info { "NegativeBalanceException for debtor: $debtor" }
                throw NegativeBalanceException("Total amount should be positive number")
            }

            updateDebtor(debtor)
            deleteLogById(logId.toLong())
            editMessageTextAndInlineKeyboard(chatId, messageId, REVERT_WAS_COMPLETED)
            showDebtorLogs(chatId, debtor.name)
        } else {
            editMessageTextAndInlineKeyboard(chatId, messageId, COMMON_ERROR)
        }
    }
}

fun setCurrency(chatId: Long, messageId: Int, text: String) {
    val match = Regex(SET_CURRENCY_PATTERN).find(text)!!
    val (currency) = match.destructured

    val newCurrency = Currency.valueOf(currency)

    connection()

    transaction {
        val user = findUserByChatId(chatId)!!
        user.defaultCurrency = newCurrency
        updateUser(user, user.chatId)
    }

    editMessageTextAndInlineKeyboard(chatId, messageId, formatCurrentCurrency(newCurrency))
}

fun unknownRequest(chatId: Long) {
    sendMessage(chatId, UNKNOWN_REQUEST)
}

private fun findDebtorsWithLogs(chatId: Long): Map<Debtor, List<Log>> {
    connection()

    val map = mutableMapOf<Debtor, List<Log>>()
    transaction {
        val debtors = findDebtorsWithCreditForUser(chatId)
        debtors.forEach {
            map[it] = findLogsForDebtorByDebtorId(it.id!!)
        }
    }

    return map
}

private fun calculateCreditAndDebit(amount: BigDecimal): Pair<BigDecimal, BigDecimal> {
    return if (amount > BigDecimal.ZERO) Pair(amount, BigDecimal.ZERO)
    else Pair(BigDecimal.ZERO, amount.multiply(BigDecimal(-1)))
}

private fun calculateAmountToAvoidNegativeBalance(totalAmount: BigDecimal, amount: BigDecimal): BigDecimal {
    if (totalAmount > BigDecimal.ZERO && amount < BigDecimal.ZERO && (totalAmount + amount < BigDecimal.ZERO)) {
        return totalAmount.multiply(BigDecimal(-1))
    }
    return amount
}


private fun newLogRecord(name: String, amount: BigDecimal, comment: String, chatId: Long): Pair<Debtor, Log> {
    connection()
    val pair = transaction {
        var debtor = findDebtorByUserIdAndName(chatId, name)
        if (debtor == null) {
            debtor = insertDebtor(chatId, name)
        }

        return@transaction insertNewLogAndRecalculateDebt(debtor, amount, comment)
    }

    return pair
}

fun insertNewLogAndRecalculateDebt(debtor: Debtor, amount: BigDecimal, comment: String): Pair<Debtor, Log> {
    logger.info { "call insertNewLogAndRecalculateDebt($debtor, $amount, $comment)" }
    connection()

    val pair = transaction {
        val recalculatedAmount = calculateAmountToAvoidNegativeBalance(debtor.totalAmount, amount)
        val (credit, debit) = calculateCreditAndDebit(recalculatedAmount)
        debtor.totalAmount += recalculatedAmount
        updateDebtor(debtor)

        val log = Log(debtor.id!!, credit, debit, comment)
        insertLog(log)

        if (debtor.totalAmount < BigDecimal.ZERO) {
            logger.info { "NegativeBalanceException for debtor: $debtor" }
            TransactionManager.current().rollback()
            throw NegativeBalanceException("Total amount should be positive number")
        }

        return@transaction Pair(debtor, log)
    }

    return pair
}
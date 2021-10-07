package by.masarnovsky.service

import by.masarnovsky.*
import by.masarnovsky.Currency
import by.masarnovsky.User
import by.masarnovsky.db.*
import by.masarnovsky.util.*
import com.elbekD.bot.types.*
import mu.KotlinLogging
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.util.*

private val logger = KotlinLogging.logger {}

fun mainMenu(chatId: Long) {
    logger.info { "main menu was called for $chatId" }
    val keyboard = createMainMenuKeyboard()
    sendMessageWithKeyboard(chatId, MAIN_MENU_DESCRIPTION, keyboard)
}

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

fun saveOrUpdateNewUser(message: Message): User {
    logger.info { "call saveOrUpdateNewUser" }
    connection()

    val user = transaction {
        var user = findUserByChatId(message.chat.id)
        if (user != null) {
            updateUser(user)
        } else {
            user = User.fromMessage(message)
            insertUser(user)
        }

        return@transaction user
    }

    return user
}

fun newDebt(chatId: Long, command: String) {
    logger.info { "call newDebt method for $chatId" }
    val match = NEW_DEBTOR_PATTERN.toRegex().find(command)!!
    val (name, amount, comment) = match.destructured
    val (debtor, _) = newLog(name, amount.toBigDecimal(), comment, chatId)

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
        val (debtor, log) = newLog(name, amount.toBigDecimal(), REPAY_VALUE, chatId)

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
    editMessageTextAndInlineKeyboard(chatId, messageId, NOT_DELETE_HISTORY, null)
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

fun deleteDebtor(chatId: Long, command: String?) {
    logger.info { "call deleteDebtor for $chatId" }
    val name = command?.replace(Regex("/delete ?"), "")

    if (name?.isNotEmpty() == true) {
        logger.info { "delete debtor $name for $chatId" }

        connection()
        val count = transaction {
            return@transaction deleteDebtorForUserByName(chatId, name)
        }

        sendMessage(chatId, constructDeleteDebtorMessageBasedOnDeletedCount(name, count))
    } else {
        logger.info { "delete all debtors for $chatId" }
        val keyboard = createDeleteAllDebtorsKeyboard()
        sendMessageWithKeyboard(chatId, DELETE_ALL_DEBTORS_WARNING, keyboard)
    }
}

fun deleteAllDebts(chatId: Long, messageId: Int) {
    logger.info { "call deleteAllDebts for $chatId" }

    connection()

    val count = transaction {
        return@transaction deleteAllDebtorsForUser(chatId)
    }

    editMessageTextAndInlineKeyboard(chatId, messageId, constructDeleteDebtorsMessageBasedOnDeletedCount(count), null)
}

fun showDebtorLogsFromCommand(chatId: Long, command: String?) {
    logger.info { "call showPersonDebts for $chatId" }
    val name = command?.replace(Regex("/show ?"), "")
    if (name?.isNotEmpty() == true) {
        showDebtorLogs(chatId, name)
    } else {
        logger.info { "/show command without name. call sendListOfDebtors for $chatId" }
        sendListOfDebtors(chatId)
    }
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

fun sendMeme(chatId: Long) {
    logger.info { "send meme for $chatId" }
    connection()

    val url = transaction {
        return@transaction findAllImages().random().url
    }

    sendImage(chatId, url)
}

private fun newLog(name: String, amount: BigDecimal, comment: String, chatId: Long): Pair<Debtor, Log> {
    logger.info { "call addNewLogToDebtor($name, $amount, $comment, $chatId)" }
    connection()

    val pair = transaction {
        var debtor = findDebtorByUserIdAndName(chatId, name)
        var (credit, debit) = calculateCreditAndDebit(amount)

        if (debtor == null) {
            debtor = Debtor(chatId, name, amount)
            debtor.id = insertDebtor(debtor)
        } else {
            val recalculatedAmount = calculateAmountToAvoidNegativeBalance(debtor.totalAmount, amount)
            val (recalculatedCredit, recalculatedDebit) = calculateCreditAndDebit(recalculatedAmount)
            credit = recalculatedCredit
            debit = recalculatedDebit
            debtor.totalAmount += recalculatedAmount
            updateDebtor(debtor)
        }

        val log = Log(debtor.id!!, credit, debit, comment)
        insertLog(log)

        if (debtor.totalAmount < BigDecimal.ZERO) {
            logger.info { "NegativeBalanceException for debtor:$debtor" }
            TransactionManager.current().rollback()
            throw NegativeBalanceException("Total amount should be positive number")
        }

        return@transaction Pair(debtor, log)
    }

    return pair
}


fun calculateAmountToAvoidNegativeBalance(totalAmount: BigDecimal, amount: BigDecimal): BigDecimal {
    if (totalAmount > BigDecimal.ZERO && amount < BigDecimal.ZERO && (totalAmount + amount < BigDecimal.ZERO)) {
        return totalAmount.multiply(BigDecimal(-1))
    }
    return amount
}

fun sendHowtoMessage(chatId: Long) {
    sendMessage(chatId, HOWTO_INFO)
}

fun mergeDebtors(chatId: Long, command: String) {
    logger.info { "call mergeDebtors method for $chatId" }

    if (isStringMatchMergePattern(command)) {
        val names = MERGE_PATTERN.toRegex().find(command)!!
        val (source, destination) = names.destructured

        connection()

        val mergedLogsCount = transaction {
            val sourceUser = findDebtorByUserIdAndName(chatId, source)
            val destinationUser = findDebtorByUserIdAndName(chatId, destination)

            if (source.equals(destination, ignoreCase = true)) {
                val duplicates = findDuplicatesByUserIdAndName(chatId, source)
                var updated = 0
                if (duplicates.size == 2) {
                    val sourceLogs = findLogsForDebtorByDebtorId(duplicates[1].id!!)
                    updated = sourceLogs
                            .map { sourceLog ->
                                newLog(
                                        duplicates[0].name,
                                        sourceLog.getAmountAsRawValue(),
                                        sourceLog.comment,
                                        chatId
                                )
                            }
                            .count()
                }
                if (updated > 0) deleteDebtorForUserById(chatId, duplicates[1].id!!)
                return@transaction updated
            } else if (sourceUser == null || destinationUser == null) {
                val existedNames = findDebtorsForUser(chatId).map { it.name }
                sendMessage(chatId, formatMergedDebtorNotFound(source, destination, existedNames))
                return@transaction 0
            } else if (sourceUser.id == destinationUser.id) {
                sendMessage(chatId, MERGE_DEBTOR_DUPLICATE_ERROR)
                return@transaction 0
            } else {
                val sourceLogs = findLogsForDebtorByDebtorId(sourceUser.id!!)
                return@transaction sourceLogs
                        .map { sourceLog ->
                            newLog(
                                    destinationUser.name,
                                    sourceLog.getAmountAsRawValue(),
                                    sourceLog.comment,
                                    chatId
                            )
                        }
                        .count()
            }
        }

        if (mergedLogsCount > 0) {
            sendMessageWithKeyboard(
                    chatId,
                    formatMergedDebtorSuccess(mergedLogsCount, source, destination),
                    createShowMergedUserKeyboard(destination)
            )
        }
    } else {
        sendMessage(chatId, WRONG_COMMAND_FORMAT)
    }
}

fun adminMergeForDebtors(command: String) {
    logger.info { "call adminMergeForDebtors method" }

    if (isStringMatchAdminMergeByDebtorIdPattern(command)) {
        val ids = ADMIN_MERGE_BY_DEBTOR_ID_PATTERN.toRegex().find(command)!!
        val (userId, source, destination) = ids.destructured
        val chatId = userId.toLong()

        connection()

        transaction {
            val sourceUser = findDebtorByUserIdAndId(chatId, source.toLong())
            val destinationUser = findDebtorByUserIdAndId(chatId, destination.toLong())
            if (sourceUser != null && destinationUser != null && sourceUser.id != destinationUser.id) {
                val sourceLogs = findLogsForDebtorByDebtorId(sourceUser.id!!)
                val mergedTransactions = sourceLogs
                    .map { sourceLog ->
                        newLog(
                                destinationUser.name,
                                sourceLog.getAmountAsRawValue(),
                                sourceLog.comment,
                                chatId
                        )
                    }.count()
                deleteDebtorForUserById(chatId, sourceUser.id!!)
                sendMessage(chatId, formatSuccessfulAdminMergeMessage(chatId, mergedTransactions, destinationUser.name, sourceUser.name))
            } else {
                sendMessage(chatId, COMMON_ERROR)
            }
        }
    }
}

fun sendMergedDebtorCallback(chatId: Long, messageId: Int, text: String) {
    val match = Regex(SHOW_MERGED_PATTERN).find(text)!!
    val (name) = match.destructured
    showDebtorLogs(chatId, name)
}

fun setCurrency(chatId: Long, messageId: Int, text: String) {
    val match = Regex(SET_CURRENCY_PATTERN).find(text)!!
    val (currency) = match.destructured

    val newCurrency = Currency.valueOf(currency)

    connection()

    transaction {
        val user = findUserByChatId(chatId)!!
        user.defaultCurrency = newCurrency
        updateUser(user)
    }

    editMessageTextAndInlineKeyboard(chatId, messageId, formatCurrentCurrency(newCurrency), null)
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
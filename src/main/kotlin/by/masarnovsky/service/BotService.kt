package by.masarnovsky.service

import by.masarnovsky.*
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
    connection()

    val map = findDebtorsWithLogs(chatId)
    val text = constructListOfAllDebtors(map)
    sendMessage(chatId, text)
}

fun saveOrUpdateNewUser(message: Message): User {
    logger.info { "call saveOrUpdateNewUser" }
    connection()

    val user = transaction {
        addLogger(StdOutSqlLogger)

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
    logger.info { "call addNewDebtor method for $chatId" }
    val match = PATTERN_NEW_DEBTOR.toRegex().find(command)!!
    val (name, amount, comment) = match.destructured
    val (debtor, _) = addNewLogToDebtor(name, amount.toBigDecimal(), comment, chatId)

    connection()
    val text = transaction {
        val logs = findLogsForDebtorByDebtorId(debtor.id!!)
        return@transaction formatDebtorRecord(debtor, logs)
    }

    sendMessage(chatId, text)
}

fun repay(chatId: Long, command: String) {
    logger.info { "call repay method for $chatId" }
    val match = Regex(PATTERN_REPAY).find(command)!!
    val (name, amount) = match.destructured
    try {
        val (debtor, _) = addNewLogToDebtor(name, amount.toBigDecimal(), REPAY_VALUE, chatId)

        connection()
        val text = transaction {
            val logs = findLogsForDebtorByDebtorId(debtor.id!!)
            return@transaction formatDebtorRecord(debtor, logs)
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

fun returnListOfDebtorsForInlineQuery(chatId: Long, queryId: String) {
    logger.info { "call returnDebtors for $chatId with queryId=$queryId" }

    val map = findDebtorsWithLogs(chatId)
    val queries = map.keys.map { debtor -> createInlineQueryResultArticle(debtor, map[debtor]!!) }
    bot.answerInlineQuery(queryId, queries)
}

fun deleteDebtor(chatId: Long, command: String?) {
    logger.info { "call deletePerson for $chatId" }
    val name = command?.replace(Regex("/delete ?"), "")

    if (name?.isNotEmpty() == true) {
        logger.info { "delete $name for $chatId" }

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

fun showDebtorLogs(chatId: Long, command: String?) {
    logger.info { "call showPersonDebts for $chatId" }
    val name = command?.replace(Regex("/show ?"), "")
    if (name?.isNotEmpty() == true) {

        connection()

        val (debtor, logs) = transaction {
            val debtor = findDebtorByUserIdAndName(chatId, name)
            val logs = if (debtor != null) findLogsForDebtorByDebtorId(debtor.id!!) else listOf()

            return@transaction Pair(debtor, logs)
        }

        if (debtor != null) {
            val header = formatDebtorHistoryHeader(debtor)
            val footer = formatDebtorHistoricalAmount(debtor, logs)
            val text = logs
                .reversed()
                .fold(header) { temp, log -> temp + log.summarize() }
                .plus(footer)
            sendMessage(chatId, text)
        } else {
            sendMessage(chatId, DEBTOR_NOT_FOUND)
        }
    } else {
        logger.info { "/show command without name. call sendListOfDebtors for $chatId" }
        sendListOfDebtors(chatId)
    }
}

private fun addNewLogToDebtor(name: String, amount: BigDecimal, comment: String, chatId: Long): Pair<Debtor, Log> {
    logger.info { "call addNewLogToDebtor($name, $amount, $comment, $chatId)" }
    connection()

    val pair = transaction {
        addLogger(StdOutSqlLogger)
        var debtor = findDebtorByUserIdAndName(chatId, name)
        val (credit, debit) = calculateCreditAndDebit(amount)

        if (debtor == null) {
            debtor = Debtor(chatId, name, amount)
            debtor.id = insertDebtor(debtor)
        } else {
            debtor.totalAmount += amount
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

fun howtoCommand(chatId: Long) {
    sendMessage(chatId, HOWTO_INFO)
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
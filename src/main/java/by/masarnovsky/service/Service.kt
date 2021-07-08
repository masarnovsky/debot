package by.masarnovsky.service

import by.masarnovsky.*
import by.masarnovsky.User
import by.masarnovsky.db.*
import com.elbekD.bot.types.*
import mu.KotlinLogging
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.util.*

private val logger = KotlinLogging.logger {}

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

fun newDebt(chatId: Long, text: String) {
    logger.info { "call addNewDebtor method for $chatId" }
    val match = PATTERN_NEW_DEBTOR.toRegex().find(text)!!
    val (name, amount, comment) = match.destructured
    val (debtor, log) = addNewLogToDebtor(name, amount.toBigDecimal(), comment, chatId)

    connection()
    val text = transaction {
        val logs = findLogsForDebtorByDebtorId(debtor.id!!)
        return@transaction formatDebtorRecord(debtor, logs)
    }

    sendMessage(chatId, text)
}

fun repay(chatId: Long, text: String) {
    logger.info { "call repay method for $chatId" }
    val match = Regex(PATTERN_REPAY).find(text)!!
    val (name, amount) = match.destructured
    try {
        val (debtor, log) = addNewLogToDebtor(name, amount.toBigDecimal(), REPAY_VALUE, chatId)

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

fun addNewLogToDebtor(name: String, amount: BigDecimal, comment: String, chatId: Long): Pair<Debtor, Log> {
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

fun calculateCreditAndDebit(amount: BigDecimal): Pair<BigDecimal, BigDecimal> {
    return if (amount > BigDecimal.ZERO) Pair(amount, BigDecimal.ZERO)
    else Pair(BigDecimal.ZERO, amount.multiply(BigDecimal(-1)))
}

fun connection(): Database {
    return Database.connect(
        url = postgresUrl,
        driver = "com.impossibl.postgres.jdbc.PGDriver",
        user = postgresUser,
        password = postgresPassword,
    )
}

fun formatDebtorRecord(debtor: Debtor, logs: List<Log>): String {
    return DEBTOR_RECORD.format(debtor.name, debtor.totalAmount, constructListOfLogs(debtor.totalAmount, logs))
}

fun formatDebtorShortRecord(debtor: Debtor, logs: List<Log>): String {
    return DEBTOR_RECORD_SHORT.format(debtor.name, debtor.totalAmount, constructListOfLogs(debtor.totalAmount, logs))
}

fun formatTotalAmountOfDebtsRecord(debtors: Set<Debtor>): String {
    return "Общая сумма долгов равна ${Debtor.totalAmount(debtors)} BYN\n"
}

fun findDebtorsWithLogs(chatId: Long): Map<Debtor, List<Log>> {
    connection()

    val map = mutableMapOf<Debtor, List<Log>>()
    transaction {
        val debtors = findDebtorsForUser(chatId)
        debtors.forEach {
            map[it] = findLogsForDebtorByDebtorId(it.id!!)
        }
    }

    return map
}

fun sendListOfDebtors(chatId: Long) {
    logger.info { "call sendListOfDebtors method for $chatId" }
    connection()

    val map = findDebtorsWithLogs(chatId)
    val text = constructListOfAllDebtors(map)
    sendMessage(chatId, text)
}

fun constructListOfAllDebtors(debtorsMap: Map<Debtor, List<Log>>): String {
    val debtors = debtorsMap.keys
    val totalAmount = formatTotalAmountOfDebtsRecord(debtors)
    val debtorsRows =
        debtors.joinToString(separator = "\n") { debtor -> formatDebtorShortRecord(debtor, debtorsMap[debtor]!!) }
    return totalAmount + debtorsRows
}

fun constructListOfLogs(totalAmount: BigDecimal, logs: List<Log>): String {
    return logs
        .sortedByDescending { it.created }
        .filter { log -> log.isEqualsToZeroAfterSubtractingFrom(totalAmount) }
        .filter { it.comment != REPAY_VALUE }
        .joinToString(", ") { log -> log.comment }
}

fun constructDeleteDebtorMessageBasedOnDeletedCount(name: String, count:Int): String {
    return if (count > 0) SUCCESSFUL_DEBTOR_REMOVAL.format(name) else DEBTOR_NOT_FOUND
}

fun constructDeleteDebtorsMessageBasedOnDeletedCount(count:Int): String {
    return if (count > 0) SUCCESSFUL_DEBTORS_REMOVAL.format(count) else DEBTORS_NOT_FOUND
}

fun mainMenu(chatId: Long) {
    logger.info { "main menu was called for $chatId" }
    val list = InlineKeyboardButton(text = "Список всех", callback_data = "callback_list")
    val keyboard = InlineKeyboardMarkup(listOf(listOf(list)))
    sendMessageWithKeyboard(chatId, MAIN_MENU_DESCRIPTION, keyboard)
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

fun createInlineQueryResultArticle(debtor: Debtor, logs:List<Log>): InlineQueryResultArticle {
    return InlineQueryResultArticle(
        id = UUID.randomUUID().toString(),
        title = debtor.name,
        input_message_content = createInputTextMessageContent(debtor, logs),
        description = DEBTOR_RECORD_FOR_INLINE_QUERY.format(debtor.name, debtor.totalAmount),
    )
}

fun createInputTextMessageContent(debtor: Debtor, logs: List<Log>): InputTextMessageContent {
    return InputTextMessageContent(
        message_text = formatDebtorRecord(debtor, logs),
        parse_mode = "HTML",
    )
}

fun deleteDebtor(chatId: Long, text: String?) {
    logger.info { "call deletePerson for $chatId" }
    val name = text?.replace(Regex("/delete ?"), "")

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

fun createDeleteAllDebtorsKeyboard(): InlineKeyboardMarkup {
    val yes = InlineKeyboardButton(text = "Да", callback_data = DELETE_HISTORY_CALLBACK)
    val no = InlineKeyboardButton(text = "Нет", callback_data = NOT_DELETE_HISTORY_CALLBACK)
    return InlineKeyboardMarkup(listOf(listOf(yes, no)))
}
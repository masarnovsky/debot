package by.masarnovsky.service

import by.masarnovsky.*
import by.masarnovsky.db.Debtors
import by.masarnovsky.db.Logs
import by.masarnovsky.db.Users
import com.elbekD.bot.types.Message
import mu.KotlinLogging
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal

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

fun findUserByChatId(chatId: Long): User? {
    logger.info { "find user by chatId:$chatId" }
    return Users
        .select { Users.id eq chatId }
        .firstOrNull()
        ?.let { User.fromRow(it) }
}

fun findDebtorByUserIdAndName(chatId: Long, name: String): Debtor? {
    logger.info { "find debtor with name $name for user $chatId" }
    return Debtors
        .select { (Debtors.userId eq chatId) and (Debtors.name eq name) }
        .firstOrNull()
        ?.let { Debtor.fromRow(it) }
}

fun findLogsForDebtorByDebtorId(debtorId: Long): List<Log> {
    logger.info { "find logs for debtor:$debtorId" }
    return Logs
        .select { Logs.debtorId eq debtorId }
        .map { Log.fromRow(it) }
}

fun insertUser(user: User): Long {
    logger.info { "save new user $user" }
    return Users.insertAndGetId {
        it[id] = user.chatId
        it[username] = user.username
        it[firstName] = user.firstName
        it[lastName] = user.lastName
        it[defaultLang] = user.defaultLang
        it[defaultCurrency] = user.defaultCurrency
        it[created] = TimeService.now()
        it[updated] = TimeService.now()
    }.value
}

fun insertDebtor(debtor: Debtor): Long {
    logger.info { "save new debtor $debtor for user ${debtor.userId}" }
    return Debtors.insertAndGetId {
        it[userId] = debtor.userId
        it[name] = debtor.name
        it[totalAmount] = debtor.totalAmount
        it[created] = debtor.created
        it[updated] = debtor.updated
    }.value
}

fun insertLog(log: Log): Long {
    logger.info { "save new log $log" }
    return Logs.insertAndGetId {
        it[debtorId] = log.debtorId
        it[credit] = log.credit
        it[debit] = log.debit
        it[created] = log.created
        it[comment] = log.comment
        it[currency] = log.currency
        it[type] = log.type
    }.value
}

fun updateUser(user: User) {
    logger.info { "update user $user" }
    Users.update({ Users.id eq user.chatId }) {
        it[id] = user.chatId
        it[username] = user.username
        it[firstName] = user.firstName
        it[lastName] = user.lastName
        it[defaultLang] = user.defaultLang
        it[defaultCurrency] = user.defaultCurrency
        it[updated] = TimeService.now()
    }
}

fun updateDebtor(debtor: Debtor) {
    logger.info { "update debtor $debtor" }
    Debtors.update({ Debtors.id eq debtor.id }) {
        it[userId] = debtor.userId
        it[name] = debtor.name
        it[totalAmount] = debtor.totalAmount
        it[created] = debtor.created
        it[updated] = TimeService.now()
    }
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

    bot.sendMessage(chatId, text)
}

fun repay(chatId: Long, text: String) {
    logger.info { "call repay method for $chatId" }
    val match = Regex(PATTERN_REPAY).find(text)!!
    val (name, amount) = match.destructured
    try {
        val (debtor, log) = addNewLogToDebtor(name, amount.toBigDecimal(), REPAY_VALUE, chatId)
    } catch (ex: NegativeBalanceException) {

    }

    //sent msg to chat
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

fun findAllUsers(): List<User> {
    logger.info { "find all users" }
    return Users.selectAll().map { User.fromRow(it) }
}

fun findDebtorsForUser(chatId: Long): List<Debtor> {
    logger.info { "find all debtor for user:$chatId" }
    return Debtors.select { Debtors.userId eq chatId }.map { Debtor.fromRow(it) }
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
    return "Теперь ${debtor.name} торчит тебе ${debtor.totalAmount} BYN за: <b>${
        formatListOfLogs(
            debtor.totalAmount,
            logs
        )
    }</b>"
}

fun formatListOfLogs(totalAmount: BigDecimal, logs: List<Log>): String {
    return logs
        .sortedByDescending { it.created }
        .filter { log -> log.isEqualsToZeroAfterSubtractingFrom(totalAmount) }
        .filter { it.comment != REPAY_VALUE }
        .joinToString(", ") { log -> log.comment }
}
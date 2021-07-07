package by.masarnovsky.service

import by.masarnovsky.*
import by.masarnovsky.db.Debtors
import by.masarnovsky.db.Logs
import by.masarnovsky.db.Users
import com.elbekD.bot.types.Message
import mu.KotlinLogging
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal

private val logger = KotlinLogging.logger {}

fun saveOrUpdateNewUser(message: Message) {

    val db = getDatabase()

    transaction {
        addLogger(StdOutSqlLogger)

        val user = findUserByChatId(message.chat.id)
        if (user != null) {
            updateUser(user)
        } else {
            insertUser(User.fromMessage(message))
        }
    }
}

fun findUserByChatId(chatId: Long): User? {
    logger.info { "find user by chatId:$chatId" }
    return Users.select { Users.chatId eq chatId }.firstOrNull()?.let { User.fromRow(it) }
}

fun findDebtorByUserIdAndName(userId: Long, name: String): Debtor? {
    logger.info { "find debtor with name $name for user $userId" }
    return Debtors
        .select { (Debtors.userId eq userId) and (Debtors.name eq name) }
        .firstOrNull()
        ?.let { Debtor.fromRow(it) }
}

fun insertUser(user: User): Long {
    logger.info { "save new user $user" }
    return Users.insertAndGetId {
        it[chatId] = user.chatId
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
    }.value
}

fun insertLog(log: Log): Long {
    logger.info { "save new transaction for deb" }
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
    Users.update({ Users.id eq user.id }) {
        it[chatId] = user.chatId
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

fun addNewDebt(chatId: Long, text: String) {
    logger.info { "call addNewDebtor method for $chatId" }
    val match = PATTERN_NEW_DEBTOR.toRegex().find(text)!!
    val (name, amount, comment) = match.destructured
//    addNewLogToDebtor(name, amount, comment)
}

fun addNewLogToDebtor(name: String, amount: BigDecimal, comment: String, userId: Long): Pair<Debtor, Log> {
    var debtor = findDebtorByUserIdAndName(userId, name)
    val (credit, debit) = calculateCreditAndDebit(amount)

    if (debtor == null) {
        debtor = Debtor(userId, name, amount)
        debtor.id = insertDebtor(debtor)
    } else {
        debtor.totalAmount += amount
        updateDebtor(debtor)
    }

    val log = Log(debtor.id!!, credit, debit, comment)
    insertLog(log)

    return Pair(debtor, log)
}

fun calculateCreditAndDebit(amount: BigDecimal): Pair<BigDecimal, BigDecimal> {
    return if (amount > BigDecimal.ZERO) Pair(amount, BigDecimal.ZERO)
    else Pair(BigDecimal.ZERO, amount.multiply(BigDecimal(-1)))
}

fun findAllUsers(): List<User> {
    logger.info { "find all users" }
    return Users.selectAll().map { User.fromRow(it) }
}

fun getDatabase(): Database {
    return Database.connect(
        url = postgresUrl,
        driver = "com.impossibl.postgres.jdbc.PGDriver",
        user = postgresUser,
        password = postgresPassword,
    )
}
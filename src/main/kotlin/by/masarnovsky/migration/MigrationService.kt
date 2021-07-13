package by.masarnovsky.migration

import by.masarnovsky.*
import by.masarnovsky.db.*
import by.masarnovsky.service.sendMessage
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import mu.KotlinLogging
import org.bson.types.ObjectId
import org.jetbrains.exposed.sql.transactions.transaction
import org.litote.kmongo.KMongo
import org.litote.kmongo.getCollection
import java.math.BigDecimal
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

fun replicateMongoUsers() {
    val client = createMongoClient()
    val usersFromMongo = client.startSession().use { clientSession ->
        clientSession.startTransaction()

        val database: MongoDatabase = client.getDatabase(database)
        val usersCollection = database.getCollection<UserMongo>("users")

        logger.info { "users from Mongo $usersCollection" }

        return@use usersCollection.find().toList().filterNotNull().map { it.fromUserMongoToUserPostgres() }
    }

    connection()

    transaction {
        val existedIds = findAllUsers().map { it.chatId }
        val usersForSaving = usersFromMongo.filter { !existedIds.contains(it.chatId) }
        logger.info { "users for storing into Postgres $usersForSaving" }
        val inserted = insertUsers(usersForSaving)
        logger.info { "saved $inserted users" }
        sendMessage(ownerId.toLong(), "replicated $inserted users")
    }

}

fun replicateMongoDebtorsAndDebts() {
    val client = createMongoClient()
    val debtorsFromMongo = client.startSession().use { clientSession ->
        clientSession.startTransaction()

        val database: MongoDatabase = client.getDatabase(database)
        val debtorsCollection = database.getCollection<DebtorMongo>("debts")

        logger.info { "debtors from Mongo $debtorsCollection" }

        return@use debtorsCollection.find().toList().filterNotNull()
    }

    connection()

    val userIds = transaction {
        return@transaction findAllUsers().map { it.chatId }
    }

    val inserted = debtorsFromMongo.map { debtorMongo ->

        return@map transaction {
            if (!userIds.contains(debtorMongo.chatId)) return@transaction 0

            val debtor = debtorMongo.fromDebtorMongoToDebtorPostgres()
            val existedDebtors = findDebtorsForUser(debtor.userId).map { it.name.toLowerCase() }
            if (!existedDebtors.contains(debtor.name.toLowerCase())) {
                val debtorId = insertDebtor(debtor)
                val logs = debtorMongo.debts.map { it.fromDebtMongoToLog(debtorId) }.map { insertLog(it) }

                logger.info { "save debtor $debtor with logs: $logs" }
                return@transaction 1
            } else {
                return@transaction 0
            }

        }
    }.sum()

    sendMessage(ownerId.toLong(), "replicated $inserted debtors")
}


data class DebtorMongo(
    var _id: ObjectId?,
    val chatId: Long,
    val name: String,
    var totalAmount: BigDecimal,
    var debts: MutableList<DebtMongo>,
) {

    fun fromDebtorMongoToDebtorPostgres(): Debtor {
        return Debtor(userId = chatId, name = name, totalAmount = debts.sumOf { it.sum })
    }
}

data class DebtMongo(val sum: BigDecimal, val comment: String, val date: LocalDateTime, var totalAmount: BigDecimal) {

    fun fromDebtMongoToLog(debtorId: Long): Log {
        return Log(
            debtorId = debtorId,
            credit = if (sum > BigDecimal.ZERO) sum else BigDecimal.ZERO,
            debit = if (sum < BigDecimal.ZERO) sum.multiply(BigDecimal(-1)) else BigDecimal.ZERO,
            created = date,
            comment = comment,
        )
    }
}

data class UserMongo(
    var _id: ObjectId?,
    val chatId: Long,
    var username: String?,
    var firstName: String?,
    var lastName: String?,
    var lastCommand: String?,
    var commandValue: String?,
    val created: LocalDateTime,
    var updated: LocalDateTime,
    var userId: Int?,
) {

    fun fromUserMongoToUserPostgres(): User {
        return User(chatId, username, firstName, lastName, created, updated)
    }
}

private fun createMongoClient(): MongoClient {
    return KMongo.createClient(databaseUrl)
}
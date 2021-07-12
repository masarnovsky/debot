package by.masarnovsky.migration

import by.masarnovsky.User
import by.masarnovsky.database
import by.masarnovsky.databaseUrl
import by.masarnovsky.db.connection
import by.masarnovsky.db.findAllUsers
import by.masarnovsky.db.insertUsers
import by.masarnovsky.ownerId
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
import java.time.ZoneOffset

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

}


data class DebtorMongo(
    var _id: ObjectId?,
    val chatId: Long,
    val name: String,
    var totalAmount: BigDecimal,
    var debts: MutableList<DebtMongo>,
) {
    constructor(chatId: Long, name: String, totalAmount: BigDecimal, debts: MutableList<DebtMongo>) : this(
        null,
        chatId,
        name,
        totalAmount,
        debts,
    )
}

data class DebtMongo(val sum: BigDecimal, val comment: String, val date: LocalDateTime, var totalAmount: BigDecimal) {
    constructor(sum: BigDecimal, comment: String, date: LocalDateTime) : this(sum, comment, date, sum)
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
    constructor(chatId: Long, username: String?, firstName: String?, lastName: String?, userId: Int?) : this(
        null,
        chatId,
        username,
        firstName,
        lastName,
        null,
        null,
        LocalDateTime.now(ZoneOffset.of("+03:00")),
        LocalDateTime.now(ZoneOffset.of("+03:00")),
        userId
    )

    fun fromUserMongoToUserPostgres(): User {
        return User(chatId, username, firstName, lastName, created, updated)
    }

    fun copyInto(user: UserMongo?): UserMongo {
        return if (user != null) {
            user.firstName = this.firstName
            user.lastName = this.lastName
            user.username = this.username
            user.updated = LocalDateTime.now(ZoneOffset.of("+03:00"))
            user
        } else {
            this
        }
    }
}

private fun createMongoClient(): MongoClient {
    return KMongo.createClient(databaseUrl)
}
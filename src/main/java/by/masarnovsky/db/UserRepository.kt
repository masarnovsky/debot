package by.masarnovsky.db

import by.masarnovsky.User
import by.masarnovsky.service.TimeService
import mu.KotlinLogging
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

private val logger = KotlinLogging.logger {}

fun findUserByChatId(chatId: Long): User? {
    logger.info { "find user by chatId:$chatId" }
    return Users
        .select { Users.id eq chatId }
        .firstOrNull()
        ?.let { User.fromRow(it) }
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

fun findAllUsers(): List<User> {
    logger.info { "find all users" }
    return Users.selectAll().map { User.fromRow(it) }
}
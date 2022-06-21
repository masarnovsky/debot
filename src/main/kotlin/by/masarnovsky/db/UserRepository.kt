package by.masarnovsky.db

import by.masarnovsky.User
import by.masarnovsky.service.TimeService
import mu.KotlinLogging
import org.jetbrains.exposed.sql.*

private val logger = KotlinLogging.logger {}

fun findUserByChatId(chatId: Long): User? {
  logger.info { "find user by chatId:$chatId" }
  return Users.select { Users.id eq chatId }.firstOrNull()?.let { User.fromRow(it) }
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
        it[created] = user.created
        it[updated] = user.updated
      }
      .value
}

fun updateUser(user: User, chatId: Long): User {
  logger.info { "update user $user" }
  Users.update({ Users.id eq chatId }) {
    it[username] = user.username
    it[firstName] = user.firstName
    it[lastName] = user.lastName
    it[defaultLang] = user.defaultLang
    it[defaultCurrency] = user.defaultCurrency
    it[updated] = TimeService.now()
  }

  return user
}

fun findAllUsers(): List<User> {
  logger.info { "find all users" }
  return Users.selectAll().map { User.fromRow(it) }
}

fun insertUsers(users: List<User>): Int {
  logger.info { "save ${users.size} users" }
  return users.map { insertUser(it) }.count()
}

package by.masarnovsky.command

import by.masarnovsky.MAIN_MENU_DESCRIPTION
import by.masarnovsky.START_COMMAND
import by.masarnovsky.User
import by.masarnovsky.db.connection
import by.masarnovsky.db.findUserByChatId
import by.masarnovsky.db.insertUser
import by.masarnovsky.db.updateUser
import by.masarnovsky.service.sendMessageWithKeyboard
import by.masarnovsky.util.createMainMenuKeyboard
import com.elbekD.bot.types.Message
import mu.KotlinLogging
import org.jetbrains.exposed.sql.transactions.transaction

private val logger = KotlinLogging.logger {}

class StartCommand : Command {
  override fun getCommandName(): String = START_COMMAND

  override fun execute(message: Message) {
    val (chatId, _) = getChatIdAndTextFromMessage(message)
    saveOrUpdateNewUser(message)
    mainMenu(chatId)
  }

  private fun saveOrUpdateNewUser(message: Message): User {
    logger.info { "call saveOrUpdateNewUser" }
    connection()

    val user = transaction {
      val dbUser = findUserByChatId(message.chat.id)
      var user = User.fromMessage(message)
      if (dbUser != null) {
        user = updateUser(user, dbUser.chatId)
      } else {
        insertUser(user)
      }

      return@transaction user
    }

    return user
  }

  private fun mainMenu(chatId: Long) {
    logger.info { "main menu was called for $chatId" }
    val keyboard = createMainMenuKeyboard()
    sendMessageWithKeyboard(chatId, MAIN_MENU_DESCRIPTION, keyboard)
  }
}

package by.masarnovsky.command

import by.masarnovsky.DELETE_ALL_DEBTORS_WARNING
import by.masarnovsky.DELETE_COMMAND
import by.masarnovsky.db.connection
import by.masarnovsky.db.deleteDebtorForUserByName
import by.masarnovsky.service.sendMessage
import by.masarnovsky.service.sendMessageWithKeyboard
import by.masarnovsky.util.constructDeleteDebtorMessageBasedOnDeletedCount
import by.masarnovsky.util.createDeleteAllDebtorsKeyboard
import com.elbekD.bot.types.Message
import mu.KotlinLogging
import org.jetbrains.exposed.sql.transactions.transaction

private val logger = KotlinLogging.logger {}

class DeleteCommand : Command {
  override fun getCommandName(): String = DELETE_COMMAND

  override fun execute(message: Message) {
    val (chatId, text) = getChatIdAndTextFromMessage(message)
    deleteDebtor(chatId, text)
  }

  private fun deleteDebtor(chatId: Long, command: String?) {
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
}

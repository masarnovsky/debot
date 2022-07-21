package by.masarnovsky.command

import by.masarnovsky.DELETE_ALL_DEBTORS_WARNING
import by.masarnovsky.DELETE_COMMAND
import by.masarnovsky.service.sendMessageWithKeyboard
import by.masarnovsky.util.createDeleteAllDebtorsKeyboard
import by.masarnovsky.util.createDeleteDebtorKeyboard
import by.masarnovsky.util.formatDeleteDebtorHistoryWarningMessage
import com.elbekD.bot.types.Message
import mu.KotlinLogging

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
      logger.info { "request to delete debtor $name for $chatId" }
      sendMessageWithKeyboard(
          chatId, formatDeleteDebtorHistoryWarningMessage(name), createDeleteDebtorKeyboard(name))
    } else {
      logger.info { "request to delete all debtors for $chatId" }
      val keyboard = createDeleteAllDebtorsKeyboard()
      sendMessageWithKeyboard(chatId, DELETE_ALL_DEBTORS_WARNING, keyboard)
    }
  }
}

package by.masarnovsky.command

import by.masarnovsky.ALL_COMMAND
import by.masarnovsky.service.sendListOfDebtors
import com.elbekD.bot.types.Message

class AllCommand : Command {
  override fun getCommandName(): String = ALL_COMMAND

  override fun execute(message: Message) {
    val (chatId, _) = getChatIdAndTextFromMessage(message)
    sendListOfDebtors(chatId)
  }
}

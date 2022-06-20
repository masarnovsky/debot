package by.masarnovsky.command

import by.masarnovsky.SHOW_COMMAND
import by.masarnovsky.service.showDebtorLogsFromCommand
import com.elbekD.bot.types.Message

class ShowCommand: Command {
    override fun getCommandName(): String = SHOW_COMMAND

    override fun execute(message: Message) {
        val (chatId, text) = getChatIdAndTextFromMessage(message)
        showDebtorLogsFromCommand(chatId, text)
    }
}
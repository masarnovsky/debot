package by.masarnovsky.command

import by.masarnovsky.REVERT_COMMAND
import by.masarnovsky.service.revertLog
import com.elbekD.bot.types.Message

class RevertCommand: Command {
    override fun getCommandName(): String = REVERT_COMMAND

    override fun execute(message: Message) {
        val (chatId, text) = getChatIdAndTextFromMessage(message)
        revertLog(chatId, text!!)
    }
}
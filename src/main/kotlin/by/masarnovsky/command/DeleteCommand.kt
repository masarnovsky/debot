package by.masarnovsky.command

import by.masarnovsky.DELETE_COMMAND
import by.masarnovsky.service.deleteDebtor
import com.elbekD.bot.types.Message

class DeleteCommand : Command {
    override fun getCommandName(): String = DELETE_COMMAND

    override fun execute(message: Message) {
        val (chatId, text) = getChatIdAndTextFromMessage(message)
        deleteDebtor(chatId, text)
    }
}
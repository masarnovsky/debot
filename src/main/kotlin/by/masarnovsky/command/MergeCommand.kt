package by.masarnovsky.command

import by.masarnovsky.MERGE_COMMAND
import by.masarnovsky.service.mergeDebtors
import com.elbekD.bot.types.Message

class MergeCommand : Command {
    override fun getCommandName(): String = MERGE_COMMAND

    override fun execute(message: Message) {
        val (chatId, text) = getChatIdAndTextFromMessage(message)
        mergeDebtors(chatId, text!!)
    }
}
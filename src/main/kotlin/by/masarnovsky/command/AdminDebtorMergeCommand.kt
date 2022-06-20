package by.masarnovsky.command

import by.masarnovsky.ADMIN_DEBTOR_MERGE_COMMAND
import by.masarnovsky.ownerId
import by.masarnovsky.service.adminMergeForDebtors
import com.elbekD.bot.types.Message

class AdminDebtorMergeCommand : Command {
    override fun getCommandName(): String = ADMIN_DEBTOR_MERGE_COMMAND

    override fun execute(message: Message) {
        val (chatId, text) = getChatIdAndTextFromMessage(message)
        if (chatId == ownerId.toLong()) {
            adminMergeForDebtors(text!!)
        }
    }
}
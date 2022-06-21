package by.masarnovsky.command

import by.masarnovsky.SHOW_COMMAND
import by.masarnovsky.service.sendListOfDebtors
import by.masarnovsky.service.showDebtorLogs
import com.elbekD.bot.types.Message
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class ShowCommand: Command {
    override fun getCommandName(): String = SHOW_COMMAND

    override fun execute(message: Message) {
        val (chatId, text) = getChatIdAndTextFromMessage(message)
        showDebtorLogsFromCommand(chatId, text)
    }

    private fun showDebtorLogsFromCommand(chatId: Long, text: String?) {
        logger.info { "call showPersonDebts for $chatId" }
        val name = text?.replace(Regex("/show ?"), "")
        if (name?.isNotEmpty() == true) {
            showDebtorLogs(chatId, name)
        } else {
            logger.info { "/show command without name. call sendListOfDebtors for $chatId" }
            sendListOfDebtors(chatId)
        }
    }
}
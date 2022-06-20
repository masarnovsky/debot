package by.masarnovsky.command

import by.masarnovsky.HOWTO_COMMAND
import by.masarnovsky.HOWTO_INFO
import by.masarnovsky.service.sendMessage
import com.elbekD.bot.types.Message

class HowToCommand : Command {
    override fun getCommandName(): String = HOWTO_COMMAND

    override fun execute(message: Message) {
        val (chatId, _) = getChatIdAndTextFromMessage(message)
        sendMessage(chatId, HOWTO_INFO)
    }
}
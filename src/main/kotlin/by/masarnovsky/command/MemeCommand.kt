package by.masarnovsky.command

import by.masarnovsky.MEME_COMMAND
import by.masarnovsky.service.sendMeme
import com.elbekD.bot.types.Message

class MemeCommand : Command {
    override fun getCommandName(): String = MEME_COMMAND

    override fun execute(message: Message) {
        val (chatId, _) = getChatIdAndTextFromMessage(message)
        sendMeme(chatId)
    }
}
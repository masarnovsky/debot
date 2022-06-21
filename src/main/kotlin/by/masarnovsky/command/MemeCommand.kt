package by.masarnovsky.command

import by.masarnovsky.MEME_COMMAND
import by.masarnovsky.db.connection
import by.masarnovsky.db.findAllImages
import by.masarnovsky.service.sendImage
import com.elbekD.bot.types.Message
import mu.KotlinLogging
import org.jetbrains.exposed.sql.transactions.transaction

private val logger = KotlinLogging.logger {}

class MemeCommand : Command {
    override fun getCommandName(): String = MEME_COMMAND

    override fun execute(message: Message) {
        val (chatId, _) = getChatIdAndTextFromMessage(message)
        sendMeme(chatId)
    }

    private fun sendMeme(chatId: Long) {
        logger.info { "send meme for $chatId" }
        connection()

        val url = transaction {
            return@transaction findAllImages().random().url
        }

        sendImage(chatId, url)
    }
}
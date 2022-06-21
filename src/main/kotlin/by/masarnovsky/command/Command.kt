package by.masarnovsky.command

import by.masarnovsky.ChatIdAndText
import com.elbekD.bot.types.Message
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

interface Command {
  fun getCommandName(): String

  fun executeCommand(message: Message) {
    logger.info { "${getCommandName()} command was called" }
    execute(message)
  }

  fun execute(message: Message)

  fun getChatIdAndTextFromMessage(message: Message): ChatIdAndText {
    return ChatIdAndText(message.chat.id, message.text)
  }
}

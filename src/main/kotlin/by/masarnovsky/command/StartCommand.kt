package by.masarnovsky.command

import by.masarnovsky.START_COMMAND
import by.masarnovsky.service.mainMenu
import by.masarnovsky.service.saveOrUpdateNewUser
import com.elbekD.bot.types.Message

class StartCommand : Command {
    override fun getCommandName(): String = START_COMMAND

    override fun execute(message: Message) {
        val (chatId, _) = getChatIdAndTextFromMessage(message)
        saveOrUpdateNewUser(message)
        mainMenu(chatId)
    }
}
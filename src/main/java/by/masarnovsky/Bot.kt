package by.masarnovsky

import com.elbekD.bot.Bot
import com.elbekD.bot.types.CallbackQuery
import com.elbekD.bot.types.InlineQuery
import com.elbekD.bot.types.Message
import mu.KotlinLogging
import java.io.FileInputStream
import java.util.*


lateinit var token: String
lateinit var username: String
lateinit var databaseUrl: String
lateinit var database: String

private val logger = KotlinLogging.logger {}

lateinit var bot: Bot

fun main() {
    loadProperties()
    bot = Bot.createPolling(username, token)
    setBehaviour()
    bot.start()
}

private fun loadProperties() {
    if (System.getenv()["IS_PROD"].toString() != "null") {
        logger.info { "setup prod environment" }
        token = System.getenv()["BOT_TOKEN"].toString()
        username = System.getenv()["BOT_USERNAME"].toString()
        databaseUrl = System.getenv()["DATABASE_URL"].toString()
        database = System.getenv()["DATABASE"].toString()
    } else {
        logger.info { "setup test environment" }
        val properties = Properties()
        val propertiesFile = System.getProperty("user.dir") + "\\test_env.properties"
        val inputStream = FileInputStream(propertiesFile)
        properties.load(inputStream)
        token = properties.getProperty("BOT_TOKEN")
        username = properties.getProperty("BOT_USERNAME")
        databaseUrl = properties.getProperty("DATABASE_URL")
        database = properties.getProperty("DATABASE")
    }
}

private fun setBehaviour() {
    startCommand()
    showAllCommand()
    showPersonDebtsCommand()
    deleteCommand()
    onInlineQuery()
    onCallbackQuery()
    onMessage()
}

fun startCommand() {
    bot.onCommand(START_COMMAND) { message, _ ->
        logger.info { "/start command was called" }

        val (chatId, _) = getChatIdAndTextFromMessage(message)

        saveOrUpdateNewUser(message)
        mainMenu(chatId)
    }
}

fun showAllCommand() {
    bot.onCommand(ALL_COMMAND) { message, _ ->

        val (chatId, _) = getChatIdAndTextFromMessage(message)
        returnListOfDebtorsForChat(chatId)
    }
}

fun showPersonDebtsCommand() {
    bot.onCommand(SHOW_COMMAND) { message, _ ->

        val (chatId, text) = getChatIdAndTextFromMessage(message)
        showPersonDebts(chatId, text)
    }
}

fun deleteCommand() {
    bot.onCommand(DELETE_COMMAND) { message, _ ->
        val (chatId, text) = getChatIdAndTextFromMessage(message)
        deletePerson(chatId, text)
    }
}

fun onInlineQuery() {
    bot.onInlineQuery { inlineQuery ->

        val (chatId, text) = getChatIdAndTextFromInlineQuery(inlineQuery)
        returnDebtors(chatId, text!!)
    }
}

fun onCallbackQuery() {
    bot.onCallbackQuery { callback ->

        val (chatId, messageId, text) = getChatIdAndTextFromCallbackQuery(callback)

        when (text) {
            DEBTORS_LIST_CALLBACK -> returnListOfDebtorsForChat(chatId)
            DELETE_HISTORY_CALLBACK -> deleteAllDebts(chatId, messageId)
            NOT_DELETE_HISTORY_CALLBACK -> notDeleteAllDebts(chatId, messageId)
            else -> returnListOfDebtorsForChat(chatId)
        }
    }
}

fun onMessage() {
    bot.onMessage { message ->

        val (chatId, text) = getChatIdAndTextFromMessage(message)

        if (text != null && isStringMatchDebtPattern(text)) {
            addNewDebtor(chatId, text)
        } else if (text != null && isStringMatchRepayPattern(text)) {
            repay(chatId, text)
        } else {
            mainMenu(chatId)
        }
    }
}

fun isStringMatchDebtPattern(str: String): Boolean {
    return Regex(PATTERN_NEW_DEBTOR) matches str
}

fun isStringMatchRepayPattern(str: String): Boolean {
    return Regex(PATTERN_REPAY) matches str
}

private fun getChatIdAndTextFromMessage(message: Message): ChatIdAndText {
    return ChatIdAndText(message.chat.id, message.text)
}

private fun getChatIdAndTextFromCallbackQuery(callback: CallbackQuery): ChatIdAndMessageIdAndText {
    return ChatIdAndMessageIdAndText(callback.message?.chat?.id!!, callback.message?.message_id!!, callback.data!!)
}

private fun getChatIdAndTextFromInlineQuery(inlineQuery: InlineQuery): ChatIdAndText {
    return ChatIdAndText(inlineQuery.from.id.toLong(), inlineQuery.id)
}

private data class ChatIdAndText(val chatId: Long, val text: String?)

private data class ChatIdAndMessageIdAndText(val chatId: Long, val messageId: Int, val text: String?)
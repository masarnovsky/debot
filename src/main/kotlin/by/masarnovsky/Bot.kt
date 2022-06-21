package by.masarnovsky

import by.masarnovsky.command.*
import by.masarnovsky.service.*
import by.masarnovsky.util.*
import com.elbekD.bot.Bot
import com.elbekD.bot.types.CallbackQuery
import com.elbekD.bot.types.InlineQuery
import com.elbekD.bot.types.Message
import mu.KotlinLogging
import java.io.FileInputStream
import java.util.*

const val IS_PROD = "IS_PROD"
const val BOT_TOKEN = "BOT_TOKEN"
const val BOT_USERNAME = "BOT_USERNAME"
const val DATABASE_URL = "DATABASE_URL"
const val DATABASE = "DATABASE"
const val OWNER_ID = "OWNER_ID"
const val HEROKU_POSTGRESQL_GOLD_URL = "HEROKU_POSTGRESQL_GOLD_URL"

lateinit var token: String
lateinit var username: String
lateinit var databaseUrl: String
lateinit var database: String
lateinit var ownerId: String
lateinit var postgresUrl: String
lateinit var postgresUser: String
lateinit var postgresPassword: String
var isProd = false

private val logger = KotlinLogging.logger {}

lateinit var bot: Bot

fun main() {
    loadProperties()
    bot = Bot.createPolling(username, token)
    setBehaviour()
    bot.start()
//    bot.sendMessage(ownerId, "Deployed (っ◔◡◔)っ \uD83E\uDD16")
}

private fun loadProperties() {
    if (System.getenv()[IS_PROD].toString() != "null") {
        logger.info { "setup prod environment" }
        isProd = true
        token = System.getenv()[BOT_TOKEN].toString()
        username = System.getenv()[BOT_USERNAME].toString()
        databaseUrl = System.getenv()[DATABASE_URL].toString()
        database = System.getenv()[DATABASE].toString()
        ownerId = System.getenv()[OWNER_ID].toString()
        postgresUrl = System.getenv()[HEROKU_POSTGRESQL_GOLD_URL].toString()
    } else {
        logger.info { "setup test environment" }
        val properties = Properties()
        val propertiesFile = System.getProperty("user.dir") + "\\test_env.properties"
        val inputStream = FileInputStream(propertiesFile)
        properties.load(inputStream)
        token = properties.getProperty(BOT_TOKEN)
        username = properties.getProperty(BOT_USERNAME)
        databaseUrl = properties.getProperty(DATABASE_URL)
        database = properties.getProperty(DATABASE)
        ownerId = properties.getProperty(OWNER_ID)
        postgresUrl = properties.getProperty(HEROKU_POSTGRESQL_GOLD_URL)
    }
    setupPostgresCredentials()
}

private fun setupPostgresCredentials() {
    val match = POSTGRES_URL_PATTERN.toRegex().find(postgresUrl)!!
    val (username, password) = match.destructured
    postgresUser = username
    postgresPassword = password
    if (postgresUrl.startsWith("postgres://")) {
        postgresUrl = postgresUrl.replaceFirst("postgres://", "jdbc:pgsql://")
    }
    if (isProd) postgresUrl += "?sslMode=Require"
}

private fun setBehaviour() {
    setUpCommand(StartCommand())
    setUpCommand(AllCommand())
    setUpCommand(ShowCommand())
    setUpCommand(DeleteCommand())
    setUpCommand(HowToCommand())
    setUpCommand(MergeCommand())
    setUpCommand(MemeCommand())
    setUpCommand(RevertCommand())

    setUpCommand(AdminDebtorMergeCommand())

    // other
    onInlineQuery()
    onCallbackQuery()
    onMessage()
}

private fun setUpCommand(command: Command) {
    bot.onCommand(command.getCommandName()) { message, _ ->
        command.executeCommand(message)
    }
}

fun onInlineQuery() {
    bot.onInlineQuery { inlineQuery ->

        val (chatId, queryId, query) = getChatIdAndQueryIdAndTextFromInlineQuery(inlineQuery)
        if (query == MEME_COMMAND) {
            returnListOfMemesForInlineQuery(chatId, queryId!!)
        } else {
            returnListOfDebtorsForInlineQuery(chatId, queryId!!)
        }
    }
}

fun onCallbackQuery() {
    bot.onCallbackQuery { callback ->

        val (chatId, messageId, text) = getChatIdAndTextFromCallbackQuery(callback)

        if (isStringMatchShowMergePattern(text!!)) {
            sendMergedDebtorCallback(chatId, text)
        } else if (isStringMatchSetCurrencyPattern(text)) {
            setCurrency(chatId, messageId, text)
        } else if (isStringMatchRevertLastLogPattern(text)) {
            processRevertLastDebtorLog(chatId, messageId, text)
        } else {
            when (text) {
                DEBTORS_LIST_CALLBACK -> sendListOfDebtors(chatId)
                DELETE_HISTORY_CALLBACK -> deleteAllDebts(chatId, messageId)
                NOT_DELETE_HISTORY_CALLBACK -> deleteAllDebtsNoOption(chatId, messageId)
                else -> sendListOfDebtors(chatId)
            }
        }
    }
}

fun onMessage() {
    bot.onMessage { message ->

        val (chatId, text) = getChatIdAndTextFromMessage(message)
        try {
            if (text != null && isStringMatchDebtPattern(text)) {
                newDebt(chatId, text)
            } else if (text != null && isStringMatchRepayPattern(text)) {
                repay(chatId, text)
            } else {
                unknownRequest(chatId)
            }
        } catch (ex: Exception) {
            logger.error { ex }
            sendMessage(chatId, COMMON_ERROR)
        }
    }
}

private fun getChatIdAndTextFromMessage(message: Message): ChatIdAndText {
    return ChatIdAndText(message.chat.id, message.text)
}

private fun getChatIdAndTextFromCallbackQuery(callback: CallbackQuery): ChatIdAndMessageIdAndText {
    return ChatIdAndMessageIdAndText(callback.message?.chat?.id!!, callback.message?.message_id!!, callback.data!!)
}

private fun getChatIdAndQueryIdAndTextFromInlineQuery(inlineQuery: InlineQuery): InlineQueryChatIdAndIdAndText {
    return InlineQueryChatIdAndIdAndText(inlineQuery.from.id.toLong(), inlineQuery.id, inlineQuery.query)
}

data class ChatIdAndText(val chatId: Long, val text: String?)

private data class InlineQueryChatIdAndIdAndText(val chatId: Long, val queryId: String?, val text: String?)

private data class ChatIdAndMessageIdAndText(val chatId: Long, val messageId: Int, val text: String?)
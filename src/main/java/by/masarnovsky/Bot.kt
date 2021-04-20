package by.masarnovsky

import com.elbekD.bot.Bot
import mu.KotlinLogging
import java.io.FileInputStream
import java.util.*


lateinit var token: String
lateinit var username: String
lateinit var databaseUrl: String
lateinit var database: String

const val PATTERN_NEW_DEBTOR = "(?<name>[\\p{L}\\s]*) (?<sum>[0-9.,]+) (?<comment>[\\p{L}\\s-!?)(.,]*)"
const val PATTERN_REPAY = "(?<name>[\\p{L}\\s]*) (?<sum>-[0-9.,]+)"
const val REPAY_VALUE = "Возврат суммы"

const val USERS_COLLECTION = "users"
const val DEBTS_COLLECTION = "debts"

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
    bot.onCommand("/start") { msg, _ ->
        logger.info { "/start command was called" }
        saveOrUpdateNewUser(msg)
        mainMenu(msg.chat.id, bot)
    }
}

fun showAllCommand() {
    bot.onCommand("/all") { msg, _ -> returnListOfDebtorsForChat(msg.chat.id, bot) }
}

fun showPersonDebtsCommand() {
    bot.onCommand("/show") { msg, _ -> showPersonDebts(msg, bot) }
}

fun deleteCommand() {
    bot.onCommand("/delete") { msg, _ -> deletePerson(msg, bot) }
}

fun onInlineQuery() {
    bot.onInlineQuery { inlineQuery ->
        returnDebtorsForInlineQuery(inlineQuery, bot)
    }
}

fun onCallbackQuery() {
    bot.onCallbackQuery { callback ->
        val data = callback.data!!
        val chatId = callback.message?.chat?.id!!

        when (data) {
            "callback_list" -> returnListOfDebtorsForChat(chatId, bot)
            "delete_history_yes" -> deleteAllDebts(chatId, bot)
            "delete_history_no" -> mainMenu(chatId, bot)
            else -> returnListOfDebtorsForChat(chatId, bot)
        }
    }
}

fun onMessage() {
    bot.onMessage { message ->
        if (message.text != null && isStringMatchDebtPattern(message.text!!)) {
            addNewDebtor(bot, message)
        } else if (message.text != null && isStringMatchRepayPattern(message.text!!)) {
            repay(bot, message)
        } else {
            mainMenu(message.chat.id, bot)
        }
    }
}
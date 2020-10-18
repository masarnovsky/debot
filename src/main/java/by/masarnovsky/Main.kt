package by.masarnovsky

import com.elbekD.bot.Bot
import com.elbekD.bot.types.InlineKeyboardButton
import com.elbekD.bot.types.InlineKeyboardMarkup
import com.elbekD.bot.types.Message
import java.io.FileInputStream
import java.util.*

var token = ""
var username = ""
var myId = ""

fun loadProperties() {
    val properties = Properties()
    val propertiesFile = System.getProperty("user.dir") + "\\values.properties"
    val inputStream = FileInputStream(propertiesFile)
    properties.load(inputStream)
    token = properties.getProperty("bot.token")
    username = properties.getProperty("bot.username")
    myId = properties.getProperty("myid")
}

fun main(args: Array<String>) {
    loadProperties()
    val bot = Bot.createPolling(username, token)

    bot.onMessage { msg ->
        mainMenu(bot, msg)
    }

    bot.onCommand("/start") { msg, _ ->
        mainMenu(bot, msg)
    }

    bot.onCallbackQuery { callback ->
        val data = callback.data!!
        val chatId = callback.message?.chat?.id!!

        println(data)
        if (data.startsWith("debtor_")) {
            val name = data.replace("debtor_", "")
            println(name)

        } else {
            when (data) {
                "callback_add" -> addDebtNote(chatId, bot)
                "callback_new" -> addNewDebtor(chatId, bot)
                "callback_exists" -> getListOfDebtors(chatId, bot)
                else -> returnListOfDebtorsForChat(chatId, bot)
            }
        }
    }

    bot.start()

}

private fun mainMenu(bot: Bot, msg: Message) {
    val add = InlineKeyboardButton(text = "Добавить", callback_data = "callback_add")
    val list = InlineKeyboardButton(text = "Список всех", callback_data = "callback_list")
    val keyboard = InlineKeyboardMarkup(listOf(listOf(add, list)))
    bot.sendMessage(
        msg.chat.id,
        "Ты можешь добавить человека в список, либо посмотреть всех, кто тебе должен",
        markup = keyboard
    )
}

fun returnListOfDebtorsForChat(chatId: Long, bot: Bot) {
    var result = ""
    getNames().forEach { (key, value) -> result += "$key $value BYN\n" }
    bot.sendMessage(chatId, result)
}

fun addDebtNote(chatId: Long, bot: Bot) {
    val new = InlineKeyboardButton(text = "Новый", callback_data = "callback_new")
    val exists = InlineKeyboardButton(text = "Выбрать из существующих", callback_data = "callback_exists")
    val keyboard = InlineKeyboardMarkup(listOf(listOf(new, exists)))
    bot.sendMessage(chatId, "Добавь нового должника или выбери уже существующего", markup = keyboard)
}

fun addNewDebtor(chatId: Long, bot: Bot) {

}

fun getListOfDebtors(chatId: Long, bot: Bot) {
    val names = mutableListOf<InlineKeyboardButton>()
    getNames().forEach { (key, _) ->
        names.add(InlineKeyboardButton(text = key, callback_data = "debtor_$key"))
    }
    val keyboard = InlineKeyboardMarkup(listOf(names))
    bot.sendMessage(chatId, "Выбери должника", markup = keyboard)
}


fun getNames(): Map<String, Double> {
    return mutableMapOf("stas" to 14.08, "max" to 6.66, "nina" to 14.88)
}
package by.masarnovsky

import com.elbekD.bot.Bot
import com.elbekD.bot.types.InlineKeyboardButton
import com.elbekD.bot.types.InlineKeyboardMarkup
import com.elbekD.bot.types.Message
import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.eq
import org.bson.Document
import java.io.FileInputStream
import java.util.*


var token = ""
var username = ""
var myId = ""
const val PATTERN_NEW_DEBTOR = "(?<name>[\\p{L}\\s]*) (?<sum>[0-9.,]+) (?<comment>[\\p{L}\\s-!?)(.,]*)"
const val PATTERN_REPAY = "(?<name>[\\p{L}\\s]*) (?<sum>-[0-9.,]+)"

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

    bot.onMessage { message ->
        if (message.text != null && isStringMatchDebtPattern(message.text!!)) {
            addNewDebtor(bot, message)
        } else if (message.text != null && isStringMatchRepayPattern(message.text!!)) {
            repay(bot, message)
        } else {
            mainMenu(bot, message)
        }
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
            // TODO ("Подумать, нужен ли вообще этот метод")
        } else {
            when (data) {
                "callback_add" -> addDebtNote(chatId, bot)
                "callback_new" -> addNewDebtorMessage(chatId, bot)
                "callback_exists" -> getListOfDebtors(chatId, bot)
                else -> returnListOfDebtorsForChat(chatId, bot)
            }
        }
    }

    bot.start()

}

private fun addNewDebtor(bot: Bot, message: Message) {
    val match = PATTERN_NEW_DEBTOR.toRegex().find(message.text!!)!!
    val (name, sum, comment) = match.destructured
    val debtor = updateDebtor(name, sum, comment, message.chat.id)!!
    bot.sendMessage(
        message.chat.id,
        "Теперь ${debtor["name"]} торчит тебе ${debtor["sum"]} BYN за ${debtor["comments"]}"
    )
}

fun repay(bot: Bot, message: Message) {
    val match = PATTERN_REPAY.toRegex().find(message.text!!)!!
    val (name, sum) = match.destructured
    val debtor = updateDebtor(name, sum, "Возврат суммы", message.chat.id)!!
    bot.sendMessage(
        message.chat.id,
        "Теперь ${debtor["name"]} должен тебе ${debtor["sum"]} BYN за ${debtor["comments"]}"
    )
}

fun updateDebtor(name: String, sum: String, comment: String, chatId: Long): Document? {
    val connectionString = MongoClientURI("mongodb://localhost:27017")
    val mongoClient = MongoClient(connectionString)
    val database: MongoDatabase = mongoClient.getDatabase("debot")
    val collection = database.getCollection("debts")
    var debtor = collection.find(Document("name", name).append("chatId", chatId)).first()
    var newSum = sum.toDouble()

    if (debtor == null) {
        debtor = Document("name", name).append("sum", newSum).append("comments", mutableListOf(comment))
            .append("chatId", chatId)
        collection.insertOne(debtor)
    } else {
        newSum = debtor.getDouble("sum") + sum.toDouble()
        debtor["sum"] = newSum
        val comments: MutableList<String> = debtor["comments"] as MutableList<String>
        comments.add(comment)
        collection.updateOne(eq("name", name), Document("\$set", debtor))
    }

    mongoClient.close()
    return debtor
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

fun addNewDebtorMessage(chatId: Long, bot: Bot) {
    bot.sendMessage(chatId, "Отправь имя, сумму и комментарий к долгу в формате {имя} {сумма} {комментарий}")
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

fun isStringMatchDebtPattern(str: String): Boolean {
    val pattern = PATTERN_NEW_DEBTOR.toRegex()
    return pattern matches str
}

fun isStringMatchRepayPattern(str: String): Boolean {
    val pattern = PATTERN_REPAY.toRegex()
    return pattern matches str
}
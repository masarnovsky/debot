package by.masarnovsky

import com.elbekD.bot.Bot
import com.elbekD.bot.types.InlineKeyboardButton
import com.elbekD.bot.types.InlineKeyboardMarkup
import com.elbekD.bot.types.Message
import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import com.mongodb.client.FindIterable
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.gt
import org.bson.Document
import java.io.FileInputStream
import java.util.*


var token = ""
var username = ""
const val PATTERN_NEW_DEBTOR = "(?<name>[\\p{L}\\s]*) (?<sum>[0-9.,]+) (?<comment>[\\p{L}\\s-!?)(.,]*)"
const val PATTERN_REPAY = "(?<name>[\\p{L}\\s]*) (?<sum>-[0-9.,]+)"

fun loadProperties() {
    val properties = Properties()
    val propertiesFile = System.getProperty("user.dir") + "\\values.properties"
    val inputStream = FileInputStream(propertiesFile)
    properties.load(inputStream)
    token = properties.getProperty("bot.token")
    username = properties.getProperty("bot.username")
}

fun main() {
//    loadProperties()
    val bot = Bot.createPolling("ddebtbot", "1288647527:AAEb-YaZ2oR5JMTWjaYmS8Mcczr_dO7X3Hg")

    bot.onMessage { msg ->
        mainMenu(bot, msg)
    }
//    bot.onMessage { message ->
//        if (message.text != null && isStringMatchDebtPattern(message.text!!)) {
//            addNewDebtor(bot, message)
//        } else if (message.text != null && isStringMatchRepayPattern(message.text!!)) {
//            repay(bot, message)
//        } else {
//            mainMenu(bot, message)
//        }
//    }
//
//    bot.onCommand("/start") { msg, _ ->
//        mainMenu(bot, msg)
//    }
//
//    bot.onCallbackQuery { callback ->
//        val data = callback.data!!
//        val chatId = callback.message?.chat?.id!!
//
//        when (data) {
//            "callback_list" -> returnListOfDebtorsForChat(chatId, bot)
//            else -> returnListOfDebtorsForChat(chatId, bot)
//        }
//    }

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
        "${debtor["name"]} вернул(а) $sum BYN и теперь торчит ${debtor["sum"]} BYN"
    )
}

fun updateDebtor(name: String, sum: String, comment: String, chatId: Long): Document? {
    val lowercaseName = name.toLowerCase()
    val connectionString = MongoClientURI("mongodb://localhost:27017")
    val mongoClient = MongoClient(connectionString)
    val database: MongoDatabase = mongoClient.getDatabase("debot")
    val collection = database.getCollection("debts")
    var debtor = collection.find(Document("name", lowercaseName).append("chatId", chatId)).first()
    var newSum = sum.toDouble()

    if (debtor == null) {
        debtor = Document("name", lowercaseName).append("sum", newSum).append("comments", mutableListOf(comment))
            .append("chatId", chatId)
        collection.insertOne(debtor)
    } else {
        newSum = debtor.getDouble("sum") + sum.toDouble()
        debtor["sum"] = newSum
        val comments: MutableList<String> = debtor["comments"] as MutableList<String>
        comments.add(comment)
        collection.updateOne(eq("name", lowercaseName), Document("\$set", debtor))
    }

    mongoClient.close()
    return debtor
}

private fun mainMenu(bot: Bot, msg: Message) {
    val list = InlineKeyboardButton(text = "Список всех", callback_data = "callback_list")
    val keyboard = InlineKeyboardMarkup(listOf(listOf(list)))
    bot.sendMessage(
        msg.chat.id,
        "Добавляй должника в таком формате: {имя} {сумма} {комментарий}, либо {имя} -{сумма} чтобы вычесть сумму долга. Кнопочка чтобы посмотреть всех",
        markup = keyboard
    )
}

fun returnListOfDebtorsForChat(chatId: Long, bot: Bot) {
    var result = ""
    val names = getDebtors()
    names.forEach { document -> result += "${document["name"]} ${document["sum"]} BYN\n" }
    bot.sendMessage(chatId, if (result.isNotEmpty()) result else "Пока что никто тебе не должен")
}

fun getDebtors(): FindIterable<Document> {
    val connectionString = MongoClientURI("mongodb://localhost:27017")
    val mongoClient = MongoClient(connectionString)
    val database: MongoDatabase = mongoClient.getDatabase("debot")
    val collection = database.getCollection("debts")
    return collection.find(gt("sum", 0))
}

fun isStringMatchDebtPattern(str: String): Boolean {
    val pattern = PATTERN_NEW_DEBTOR.toRegex()
    return pattern matches str
}

fun isStringMatchRepayPattern(str: String): Boolean {
    val pattern = PATTERN_REPAY.toRegex()
    return pattern matches str
}
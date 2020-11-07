package by.masarnovsky

import com.elbekD.bot.Bot
import com.elbekD.bot.types.InlineKeyboardButton
import com.elbekD.bot.types.InlineKeyboardMarkup
import com.elbekD.bot.types.Message
import com.mongodb.BasicDBObject
import com.mongodb.client.MongoDatabase
import mu.KotlinLogging
import org.litote.kmongo.*
import java.io.FileInputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


var token = ""
var username = ""
var databaseUrl = ""
var database = ""

const val PATTERN_NEW_DEBTOR = "(?<name>[\\p{L}\\s]*) (?<sum>[0-9.,]+) (?<comment>[\\p{L}\\s-!?)(.,]*)"
const val PATTERN_REPAY = "(?<name>[\\p{L}\\s]*) (?<sum>-[0-9.,]+)"
const val REPAY_VALUE = "Возврат суммы"

const val USERS_COLLECTION = "users"
const val DEBTS_COLLECTION = "debts"

private val logger = KotlinLogging.logger {}

fun loadProperties() {
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

fun main() {
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
        logger.info { "/start command was called" }
        saveOrUpdateNewUser(msg)
        mainMenu(bot, msg)
    }

    bot.onCommand("/all") { msg, _ -> returnListOfDebtorsForChat(msg.chat.id, bot) }

    bot.onCommand("/show") { msg, _ -> showPersonDebts(msg, bot) }

    bot.onCommand("/delete") { msg, _ -> deletePerson(msg, bot) }

    bot.onCallbackQuery { callback ->
        val data = callback.data!!
        val chatId = callback.message?.chat?.id!!

        when (data) {
            "callback_list" -> returnListOfDebtorsForChat(chatId, bot)
            else -> returnListOfDebtorsForChat(chatId, bot)
        }
    }

    bot.start()
}

fun saveOrUpdateNewUser(msg: Message) {
    val chatId = msg.chat.id
    val username = msg.chat.username
    val firstName = msg.chat.first_name
    val lastName = msg.chat.last_name
    logger.info { "save or update method was added with parameters: $chatId, $username, $firstName, $lastName" }

    KMongo.createClient(databaseUrl).use { client ->
        val database: MongoDatabase = client.getDatabase(database)
        val collection = database.getCollection<User>(USERS_COLLECTION)

        var user = collection.findOne(User::chatId eq chatId)
        if (user == null) {
            logger.info { "insert new user" }
            user = User(chatId, username, firstName, lastName)
        } else {
            logger.info { "update existed user" }
            user.firstName = firstName
            user.lastName = lastName
            user.username = username
            user.updated = LocalDateTime.now()
        }
        collection.save(user)
    }
}

fun deletePerson(msg: Message, bot: Bot) {
    logger.info { "call deletePerson for ${msg.chat.id}" }
    val name = msg.text?.replace("/delete ", "")
    KMongo.createClient(databaseUrl).use { client ->
        val database: MongoDatabase = client.getDatabase(database)
        val collection = database.getCollection<Debtor>(DEBTS_COLLECTION)
        val whereQuery = BasicDBObject(mapOf("chatId" to msg.chat.id, "name" to name?.toLowerCase()))
        val deletedCount = collection.deleteOne(whereQuery).deletedCount
        bot.sendMessage(
            msg.chat.id,
            if (deletedCount > 0) "Информация о должнике $name была удалена" else "Вы забыли имя, либо по такому имени ничего не найдено"
        )
    }
}

fun showPersonDebts(msg: Message, bot: Bot) {
    logger.info { "call showPersonDebts for ${msg.chat.id}" }
    if ("/show" != msg.text) {
        val name = msg.text?.replace("/show ", "")
        KMongo.createClient(databaseUrl).use { client ->
            val database: MongoDatabase = client.getDatabase(database)
            val collection = database.getCollection<Debtor>(DEBTS_COLLECTION)
            val whereQuery = BasicDBObject(mapOf("chatId" to msg.chat.id, "name" to name?.toLowerCase()))
            val debtor = collection.findOne(whereQuery)

            if (debtor != null) {
                var result = "Текущий долг для ${debtor.name} равняется ${debtor.totalAmount}\nИстория долгов:\n"
                debtor.debts.reversed()
                    .forEach { debt ->
                        result += "${
                            debt.date.format(
                                DateTimeFormatter
                                    .ofPattern("yyyy-MM-dd HH:mm:ss")
                            )
                        } |    ${debt.sum} за ${debt.comment}\n"
                    }
                bot.sendMessage(msg.chat.id, result)
            } else {
                bot.sendMessage(msg.chat.id, "По такому имени ничего не найдено")
            }
        }
    } else {
        logger.info { "/show command without name. call returnListOfDebtorsForChat for ${msg.chat.id}" }
        returnListOfDebtorsForChat(msg.chat.id, bot)
    }
}

private fun addNewDebtor(bot: Bot, message: Message) {
    logger.info { "call addNewDebtor method for ${message.chat.id}" }
    val match = PATTERN_NEW_DEBTOR.toRegex().find(message.text!!)!!
    val (name, sum, comment) = match.destructured
    val debtor = updateDebtor(name, sum, comment, message.chat.id)
    bot.sendMessage(
        message.chat.id,
        "Теперь ${debtor.name} торчит тебе ${debtor.totalAmount} BYN"
    )
}

fun repay(bot: Bot, message: Message) {
    logger.info { "call repay method for ${message.chat.id}" }
    val match = PATTERN_REPAY.toRegex().find(message.text!!)!!
    val (name, sum) = match.destructured
    val debtor = updateDebtor(name, sum, REPAY_VALUE, message.chat.id)
    bot.sendMessage(
        message.chat.id,
        "${debtor.name} вернул(а) $sum BYN и теперь торчит ${debtor.totalAmount} BYN"
    )
}

fun updateDebtor(name: String, sumValue: String, comment: String, chatId: Long): Debtor {
    logger.info { "call updateDebtor method for $chatId" }
    val lowercaseName = name.toLowerCase()
    val sum = sumValue.toDouble()
    val debt = Debt(sum, comment, LocalDateTime.now())

    KMongo.createClient(databaseUrl).use { client ->
        val database: MongoDatabase = client.getDatabase(database)
        val collection = database.getCollection<Debtor>(DEBTS_COLLECTION)
        val whereQuery = BasicDBObject(mapOf("chatId" to chatId, "name" to lowercaseName))
        var debtor = collection.findOne(whereQuery)

        if (debtor != null) {
            logger.info { "update existed debtor for $chatId" }
            debtor.totalAmount += sum
            debt.totalAmount = debtor.totalAmount
            debtor.debts.add(debt)
        } else {
            logger.info { "create new debtor for $chatId" }
            debt.totalAmount = sum
            debtor = Debtor(chatId, lowercaseName, sum, mutableListOf(debt))
        }
        collection.save(debtor)

        return debtor
    }
}

private fun mainMenu(bot: Bot, msg: Message) {
    logger.info { "main menu was called for ${msg.chat.id}" }
    val list = InlineKeyboardButton(text = "Список всех", callback_data = "callback_list")
    val keyboard = InlineKeyboardMarkup(listOf(listOf(list)))
    bot.sendMessage(
        msg.chat.id,
        "Добавляй должника в таком формате: \n<b>имя 66.6 комментарий</b> \nЧтобы вычесть сумму долга: \n<b>имя -97</b> \nКнопочка чтобы посмотреть всех",
        markup = keyboard,
        parseMode = "HTML"
    )
}

fun returnListOfDebtorsForChat(chatId: Long, bot: Bot) {
    logger.info { "call returnListOfDebtorsForChat method for $chatId" }
    var result = ""
    val debtors = getDebtors(chatId)
    debtors.forEach { debtor -> result += "${debtor.name} ${debtor.totalAmount} BYN за: ${formatDebts(debtor.debts)}\n" }
    bot.sendMessage(chatId, if (result.isNotEmpty()) result else "Пока что никто тебе не должен")
}

fun formatDebts(debts: MutableList<Debt>): String {
    return debts
        .sortedByDescending { it.date }
        .map { debt -> debt.comment }
        .filter { it != REPAY_VALUE }
        .joinToString(", ")
}

fun getDebtors(chatId: Long): List<Debtor> {
    logger.info { "method getDebtors was called" }
    KMongo.createClient(databaseUrl).use { client ->
        val db = client.getDatabase(database)
        val collection = db.getCollection<Debtor>(DEBTS_COLLECTION)
        val whereQuery = BasicDBObject(mapOf("chatId" to chatId, "totalAmount" to BasicDBObject("\$gt", 0)))
        return collection.find(whereQuery).toList()
    }
}

fun isStringMatchDebtPattern(str: String): Boolean {
    val pattern = PATTERN_NEW_DEBTOR.toRegex()
    return pattern matches str
}

fun isStringMatchRepayPattern(str: String): Boolean {
    val pattern = PATTERN_REPAY.toRegex()
    return pattern matches str
}
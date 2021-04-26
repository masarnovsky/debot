package by.masarnovsky

import com.elbekD.bot.types.*
import com.mongodb.BasicDBObject
import com.mongodb.ReadConcern
import com.mongodb.WriteConcern
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import mu.KotlinLogging
import org.litote.kmongo.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val logger = KotlinLogging.logger {}

fun returnDebtors(chatId: Long, queryId: String) {
    logger.info { "call returnDebtors for $chatId with queryId=$queryId" }

    val queries = getDebtors(chatId).mapIndexed { index, debtor -> createInlineQueryResultArticle(index, debtor) }
    bot.answerInlineQuery(queryId, queries)
}

fun saveOrUpdateNewUser(message: Message) {
    var user =
        User(message.chat.id, message.chat.username, message.chat.first_name, message.chat.last_name, message.from?.id)
    logger.info { "save or update user: $user" }

    val client = createMongoClient()
    client.startSession().use { clientSession ->
        clientSession.startTransaction()

        val database: MongoDatabase = client.getDatabase(database)
        val collection = database.getCollection<User>(USERS_COLLECTION)

        collection
            .findOne { User::chatId eq user.chatId }
            .let { user.copyInto(it) }
            .apply { collection.save(this) }

        clientSession.commitTransaction()
    }
}

fun deletePerson(chatId: Long, text: String?) {
    logger.info { "call deletePerson for $chatId" }
    val name = text?.replace(Regex("/delete ?"), "")

    if (name?.isNotEmpty() == true) {
        val client = createMongoClient()
        client.startSession().use { clientSession ->
            clientSession.startTransaction()

            logger.info { "delete $name for $chatId" }
            val database: MongoDatabase = client.getDatabase(database)
            val collection = database.getCollection<Debtor>(DEBTS_COLLECTION)
            val whereQuery = BasicDBObject(mapOf("chatId" to chatId, "name" to name.toLowerCase()))
            val deletedCount = collection.withWriteConcern(WriteConcern.MAJORITY).deleteOne(whereQuery).deletedCount
            bot.sendMessage(
                chatId,
                if (deletedCount > 0) "Информация о должнике $name была удалена" else "По такому имени ничего не найдено"
            )

            clientSession.commitTransaction()
        }
    } else {
        logger.info { "delete all debtors for $chatId" }
        val yes = InlineKeyboardButton(text = "Да", callback_data = DELETE_HISTORY_CALLBACK)
        val no = InlineKeyboardButton(text = "Нет", callback_data = NOT_DELETE_HISTORY_CALLBACK)
        val keyboard = InlineKeyboardMarkup(listOf(listOf(yes, no)))
        bot.sendMessage(
            chatId,
            "Вы точно хотите удалить <b>всех</b> должников?",
            markup = keyboard,
            parseMode = "HTML"
        )
    }
}

fun deleteAllDebts(chatId: Long, messageId: Int) {
    logger.info { "call deleteAllDebts for $chatId" }

    val client = createMongoClient()
    val deletedCount = client.startSession().use { clientSession ->
        clientSession.startTransaction()

        val database: MongoDatabase = client.getDatabase(database)
        val collection = database.getCollection<Debtor>(DEBTS_COLLECTION)
        val whereQuery = BasicDBObject(mapOf("chatId" to chatId))
        val deletedCount = collection.withWriteConcern(WriteConcern.MAJORITY).deleteMany(whereQuery).deletedCount

        clientSession.commitTransaction()

        deletedCount
    }

    bot.editMessageReplyMarkup(chatId, messageId)
    bot.editMessageText(
        chatId = chatId,
        messageId = messageId,
        text = if (deletedCount > 0) "Информация о $deletedCount должниках была удалена" else "Должников не найдено"
    )
}

fun notDeleteAllDebts(chatId: Long, messageId: Int) {
    logger.info { "call notDeleteAllDebts for $chatId" }
    bot.editMessageReplyMarkup(chatId, messageId)
    bot.editMessageText(
        chatId = chatId,
        messageId = messageId,
        text = "Вы решили не удалять историю"
    )
}

fun showPersonDebts(chatId: Long, text: String?) {
    logger.info { "call showPersonDebts for $chatId" }
    val name = text?.replace(Regex("/show ?"), "")
    if (name?.isNotEmpty() == true) {
        logger.info { "show $name debts for $chatId" }

        val client = createMongoClient()
        val debtor = client.startSession().use { clientSession ->
            clientSession.startTransaction()

            val database = client.getDatabase(database)
            val collection = database.getCollection<Debtor>(DEBTS_COLLECTION)
            val whereQuery = BasicDBObject(mapOf("chatId" to chatId, "name" to name.toLowerCase()))
            val debtor = collection.withReadConcern(ReadConcern.MAJORITY).findOne(whereQuery)

            clientSession.commitTransaction()

            debtor
        }

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
            bot.sendMessage(chatId, result)
        } else {
            bot.sendMessage(chatId, "По такому имени ничего не найдено")
        }
    } else {
        logger.info { "/show command without name. call returnListOfDebtorsForChat for $chatId" }
        returnListOfDebtorsForChat(chatId)
    }
}

fun addNewDebt(chatId: Long, text: String?) {
    logger.info { "call addNewDebtor method for $chatId" }
    val match = PATTERN_NEW_DEBTOR.toRegex().find(text!!)!!
    val (name, sum, comment) = match.destructured
    val debtor = updateDebt(name, sum, comment, chatId)
    bot.sendMessage(
        chatId,
        "Теперь ${debtor.name} торчит тебе ${debtor.totalAmount} BYN за: <b>${formatDebts(debtor.debts, false)}</b>",
        parseMode = "HTML",
    )
}

fun repay(chatId: Long, text: String?) {
    logger.info { "call repay method for $chatId" }
    val match = Regex(PATTERN_REPAY).find(text!!)!!
    val (name, sum) = match.destructured
    val repayInfo = try {
        val debtor = updateDebt(name, sum, REPAY_VALUE, chatId)
        "${debtor.name} вернул(а) ${
            sum.toBigDecimal().multiply(BigDecimal(-1))
        } BYN и теперь " + if (debtor.totalAmount > BigDecimal.ZERO) "торчит ${debtor.totalAmount} BYN за: <b>${
            formatDebts(
                debtor.debts,
                false
            )
        }</b>" else "ничего не должен"
    } catch (ex: NegativeBalanceException) {
        "Введена неверная сумма, баланс не может быть отрицательным"
    }
    bot.sendMessage(
        chatId,
        repayInfo,
        parseMode = "HTML",
    )
}

fun updateDebt(name: String, sumValue: String, comment: String, chatId: Long): Debtor {
    logger.info { "call updateDebtor method for $chatId" }
    val lowercaseName = name.toLowerCase()
    val sum = sumValue.toBigDecimal()
    val debt = Debt(sum, comment, LocalDateTime.now())

    val client = createMongoClient()
    val debtor = client.startSession().use { clientSession ->
        clientSession.startTransaction()

        val collection = client.getDatabase(database).getCollection<Debtor>(DEBTS_COLLECTION)
        val whereQuery = BasicDBObject(mapOf("chatId" to chatId, "name" to lowercaseName))
        var debtor = collection.withReadConcern(ReadConcern.MAJORITY).findOne(clientSession, whereQuery)

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

        if (debtor.totalAmount < BigDecimal.ZERO) {
            clientSession.commitTransaction()
            throw NegativeBalanceException("Total amount should be positive number")
        } else {
            collection.withWriteConcern(WriteConcern.MAJORITY).save(clientSession, debtor)
            clientSession.commitTransaction()
        }
        debtor
    }

    return debtor
}

fun mainMenu(chatId: Long) {
    logger.info { "main menu was called for $chatId" }
    val list = InlineKeyboardButton(text = "Список всех", callback_data = "callback_list")
    val keyboard = InlineKeyboardMarkup(listOf(listOf(list)))
    bot.sendMessage(
        chatId,
        "Добавляй должника в таком формате: \n<b>имя 66.6 комментарий</b> \nЧтобы вычесть сумму долга: \n<b>имя -97</b> \nКнопочка чтобы посмотреть всех",
        markup = keyboard,
        parseMode = "HTML"
    )
}

fun returnListOfDebtorsForChat(chatId: Long) {
    logger.info { "call returnListOfDebtorsForChat method for $chatId" }
    val debtors = getDebtors(chatId)
    val result = debtors.fold("") { result, debtor ->
        result + "${debtor.name} ${debtor.totalAmount} BYN за: ${formatDebts(debtor.debts, false)}\n"
    }
    bot.sendMessage(chatId, if (result.isNotEmpty()) result else "Пока что никто тебе не должен")
}

fun formatDebts(debts: MutableList<Debt>, isFullDebtsOutput: Boolean = true): String {
    return if (isFullDebtsOutput) {
        logger.info { "format debts output for all items" }
        debts
            .sortedByDescending { it.date }
            .map { debt -> debt.comment }
            .filter { it != REPAY_VALUE }
            .joinToString(", ")
    } else {
        logger.info { "format debts output for last items" }
        var totalAmount = BigDecimal.ZERO

        debts
            .sortedByDescending { it.date }
            .filterIndexed { index, s ->
                if (index == 0)
                    totalAmount = s.totalAmount
                if (s.comment != REPAY_VALUE)
                    totalAmount -= s.sum
                totalAmount + s.sum > BigDecimal.ZERO
            }
            .filter { it.comment != REPAY_VALUE }
            .joinToString(", ") { debt -> debt.comment }
    }
}

fun getDebtors(chatId: Long): List<Debtor> {
    logger.info { "method getDebtors was called" }

    val client = createMongoClient()
    client.startSession().use { clientSession ->
        clientSession.startTransaction()

        val db = client.getDatabase(database)
        val collection = db.getCollection<Debtor>(DEBTS_COLLECTION)
        val whereQuery = BasicDBObject(mapOf("chatId" to chatId, "totalAmount" to BasicDBObject("\$gt", 0)))
        val debtors = collection.withReadConcern(ReadConcern.MAJORITY).find(whereQuery).toList()

        clientSession.commitTransaction()

        return debtors
    }
}

fun createInlineQueryResultArticle(index: Int, debtor: Debtor): InlineQueryResultArticle {
    return InlineQueryResultArticle(
        id = index.toString(),
        title = debtor.name,
        input_message_content = createInputTextMessageContent(debtor),
        description = "${debtor.name} торчит тебе ${debtor.totalAmount} BYN",
    )
}

fun createInputTextMessageContent(debtor: Debtor): InputTextMessageContent {
    return InputTextMessageContent(
        message_text = "${debtor.name} торчит тебе ${debtor.totalAmount} BYN за: <b>${
            formatDebts(
                debtor.debts,
                false
            )
        }</b>",
        parse_mode = "HTML",
    )
}

private fun createMongoClient(): MongoClient {
    return KMongo.createClient(databaseUrl)
}
package by.masarnovsky

import by.masarnovsky.service.sendListOfDebtors
import com.elbekD.bot.types.InlineKeyboardButton
import com.elbekD.bot.types.InlineKeyboardMarkup
import com.mongodb.BasicDBObject
import com.mongodb.ReadConcern
import com.mongodb.WriteConcern
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import mu.KotlinLogging
import org.litote.kmongo.KMongo
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection
import java.time.format.DateTimeFormatter

private val logger = KotlinLogging.logger {}

fun showPersonDebts(chatId: Long, text: String?) {
    logger.info { "call showPersonDebts for $chatId" }
    val name = text?.replace(Regex("/show ?"), "")
    if (name?.isNotEmpty() == true) {
        logger.info { "show $name debts for $chatId" }

        val client = createMongoClient()
        val debtor = client.startSession().use { clientSession ->
            clientSession.startTransaction()

            val database = client.getDatabase(database)
            val collection = database.getCollection<DebtorM>(DEBTS_COLLECTION)
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
        logger.info { "/show command without name. call sendListOfDebtors for $chatId" }
        sendListOfDebtors(chatId)
    }
}

@Deprecated(message = "old")
private fun createMongoClient(): MongoClient {
    return KMongo.createClient(databaseUrl)
}
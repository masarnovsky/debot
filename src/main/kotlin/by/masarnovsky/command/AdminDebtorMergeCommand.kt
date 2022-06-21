package by.masarnovsky.command

import by.masarnovsky.ADMIN_DEBTOR_MERGE_COMMAND
import by.masarnovsky.ADMIN_MERGE_BY_DEBTOR_ID_PATTERN
import by.masarnovsky.COMMON_ERROR
import by.masarnovsky.db.connection
import by.masarnovsky.db.deleteDebtorForUserById
import by.masarnovsky.db.findDebtorByUserIdAndId
import by.masarnovsky.ownerId
import by.masarnovsky.service.mergeDebtorsById
import by.masarnovsky.service.sendMessage
import by.masarnovsky.util.formatSuccessfulAdminMergeMessage
import by.masarnovsky.util.isStringMatchAdminMergeByDebtorIdPattern
import com.elbekD.bot.types.Message
import mu.KotlinLogging
import org.jetbrains.exposed.sql.transactions.transaction

private val logger = KotlinLogging.logger {}

class AdminDebtorMergeCommand : Command {
    override fun getCommandName(): String = ADMIN_DEBTOR_MERGE_COMMAND

    override fun execute(message: Message) {
        val (chatId, text) = getChatIdAndTextFromMessage(message)
        if (chatId == ownerId.toLong()) {
            adminMergeForDebtors(text!!)
        }
    }

    fun adminMergeForDebtors(command: String) {
        logger.info { "call adminMergeForDebtors method" }

        if (isStringMatchAdminMergeByDebtorIdPattern(command)) {
            val ids = ADMIN_MERGE_BY_DEBTOR_ID_PATTERN.toRegex().find(command)!!
            val (userId, source, destination) = ids.destructured
            val chatId = userId.toLong()

            connection()

            transaction {
                val sourceUser = findDebtorByUserIdAndId(chatId, source.toLong())
                val destinationUser = findDebtorByUserIdAndId(chatId, destination.toLong())
                if (sourceUser != null && destinationUser != null && sourceUser.id != destinationUser.id) {
                    val mergedTransactions = mergeDebtorsById(sourceUser, destinationUser)
                    deleteDebtorForUserById(chatId, sourceUser.id!!)
                    sendMessage(
                        chatId,
                        formatSuccessfulAdminMergeMessage(
                            chatId,
                            mergedTransactions,
                            destinationUser.name,
                            sourceUser.name
                        )
                    )
                } else {
                    sendMessage(chatId, COMMON_ERROR)
                }
            }
        }
    }
}
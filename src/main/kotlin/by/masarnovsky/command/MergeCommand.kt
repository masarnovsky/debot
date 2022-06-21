package by.masarnovsky.command

import by.masarnovsky.*
import by.masarnovsky.db.*
import by.masarnovsky.service.insertNewLogAndRecalculateDebt
import by.masarnovsky.service.mergeDebtorsById
import by.masarnovsky.service.sendMessage
import by.masarnovsky.service.sendMessageWithKeyboard
import by.masarnovsky.util.createShowMergedUserKeyboard
import by.masarnovsky.util.formatMergedDebtorNotFound
import by.masarnovsky.util.formatMergedDebtorSuccess
import by.masarnovsky.util.isStringMatchMergePattern
import com.elbekD.bot.types.Message
import mu.KotlinLogging
import org.jetbrains.exposed.sql.transactions.transaction

private val logger = KotlinLogging.logger {}

class MergeCommand : Command {
    override fun getCommandName(): String = MERGE_COMMAND

    override fun execute(message: Message) {
        val (chatId, text) = getChatIdAndTextFromMessage(message)
        mergeDebtors(chatId, text!!)
    }

    private fun mergeDebtors(chatId: Long, command: String) {
        logger.info { "call mergeDebtors method for $chatId" }

        if (isStringMatchMergePattern(command)) {
            val names = MERGE_PATTERN.toRegex().find(command)!!
            val (source, destination) = names.destructured

            connection()

            val (sourceUser, destinationUser) = transaction {
                val sourceUser = findDebtorByUserIdAndName(chatId, source)
                val destinationUser = findDebtorByUserIdAndName(chatId, destination)

                return@transaction Pair(sourceUser, destinationUser)
            }

            val mergedLogsCount = if (source.equals(destination, ignoreCase = true)) {
                mergeDuplicates(chatId, source)
            } else if (sourceUser == null || destinationUser == null) {
                suggestDebtorsForMerge(chatId, source, destination)
            } else if (sourceUser.id == destinationUser.id) {
                sendMessage(chatId, MERGE_DEBTOR_DUPLICATE_ERROR)
                0
            } else {
                mergeDebtorsById(sourceUser, destinationUser)
            }

            checkMergedLogsCountAndSendMessage(mergedLogsCount, chatId, source, destination)

        } else {
            sendMessage(chatId, WRONG_COMMAND_FORMAT)
        }
    }

    private fun mergeDuplicates(chatId: Long, source: String): Int {
        logger.info { "call mergeDuplicates($chatId, $source)" }

        connection()
        var updated = 0
        transaction {
            val duplicates = findDuplicatesByUserIdAndName(chatId, source)
            if (duplicates.size == 2) {
                val sourceLogs = findLogsForDebtorByDebtorId(duplicates[1].id!!)
                updated = sourceLogs
                    .map { sourceLog ->
                        insertNewLogAndRecalculateDebt(
                            duplicates[0],
                            sourceLog.getAmountAsRawValue(),
                            sourceLog.comment
                        )
                    }
                    .count()
            } else {
                sendMessage(chatId, DUPLICATES_NOT_FOUND)
            }
            if (updated > 0) deleteDebtorForUserById(chatId, duplicates[1].id!!)
        }
        return updated
    }

    private fun suggestDebtorsForMerge(chatId: Long, source: String, destination: String): Int {
        logger.info { "call suggestDebtorsForMerge($chatId, $source, $destination)" }

        connection()
        val existedNames = transaction {
            return@transaction findDebtorsForUser(chatId).map { it.name }
        }
        sendMessage(chatId, formatMergedDebtorNotFound(source, destination, existedNames))
        return 0
    }

    private fun checkMergedLogsCountAndSendMessage(
        mergedLogsCount: Int,
        chatId: Long,
        source: String,
        destination: String
    ) {
        if (mergedLogsCount > 0) {
            sendMessageWithKeyboard(
                chatId,
                formatMergedDebtorSuccess(mergedLogsCount, source, destination),
                createShowMergedUserKeyboard(destination)
            )
        }
    }
}
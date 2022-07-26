package by.masarnovsky.command

import by.masarnovsky.*
import by.masarnovsky.db.connection
import by.masarnovsky.db.findDebtorByUserIdAndName
import by.masarnovsky.db.findLastLogForDebtorByDebtorId
import by.masarnovsky.service.sendMessage
import by.masarnovsky.service.sendMessageWithKeyboard
import by.masarnovsky.util.createDeleteLastDebtorLogKeyboard
import by.masarnovsky.util.formatDeleteLastDebtorLogMessage
import by.masarnovsky.util.isStringMatchRevertPattern
import com.elbekD.bot.types.Message
import mu.KotlinLogging
import org.jetbrains.exposed.sql.transactions.transaction

private val logger = KotlinLogging.logger {}

class RevertCommand : Command {
  override fun getCommandName(): String = REVERT_COMMAND

  override fun execute(message: Message) {
    val (chatId, text) = getChatIdAndTextFromMessage(message)
    revertLog(chatId, text!!)
  }

  private fun revertLog(chatId: Long, command: String) {
    logger.info { "call revertLog for $chatId with command: $command" }

    if (isStringMatchRevertPattern(command)) {
      val (name) = REVERT_PATTERN.toRegex().find(command)!!.destructured

      connection()

      transaction {
        val debtor = findDebtorByUserIdAndName(chatId, name)
        if (debtor == null) {
          sendMessage(chatId, DEBTOR_NOT_FOUND)
        } else {
          val log = findLastLogForDebtorByDebtorId(debtor.id!!)
          if (log != null) {
            sendMessageWithKeyboard(
                chatId,
                formatDeleteLastDebtorLogMessage(debtor.name, log),
                createDeleteLastDebtorLogKeyboard(debtor.id!!, log.id!!))
          } else {
            sendMessage(chatId, DEBTOR_HAS_NO_DEBTS)
          }
        }
      }
    } else {
      sendMessage(chatId, WRONG_FORMAT_FOR_REVERT_COMMAND)
    }
  }
}

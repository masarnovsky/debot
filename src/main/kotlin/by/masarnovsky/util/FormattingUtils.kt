package by.masarnovsky.util

import by.masarnovsky.*
import by.masarnovsky.Log.Companion.calculateHistoricalCredit
import java.math.BigDecimal

fun formatNewLogRecord(debtor: Debtor, currency: Currency, logs: List<Log>): String {
  return DEBTOR_RECORD.format(
      debtor.name, debtor.totalAmount, currency.name, constructListOfLogs(debtor.totalAmount, logs))
}

fun formatRepayRecord(debtor: Debtor, log: Log, logs: List<Log>, currency: Currency): String {
  val end =
      if (debtor.totalAmount > BigDecimal.ZERO)
          DEBTOR_CREDIT_AFTER_REPAY.format(
              debtor.totalAmount, currency.name, constructListOfLogs(debtor.totalAmount, logs))
      else DEBTOR_ZERO_CREDIT

  return DEBTOR_RETURN_RECORD.format(debtor.name, log.debit, currency.name).plus(end)
}

fun formatDebtorRecordForInlineQuery(debtor: Debtor, logs: List<Log>, currency: Currency): String {
  return DEBTOR_RECORD_FOR_INLINE_QUERY.format(
      debtor.name, debtor.totalAmount, currency.name, constructListOfLogs(debtor.totalAmount, logs))
}

fun formatDebtorShortRecord(debtor: Debtor, logs: List<Log>, currency: Currency): String {
  return DEBTOR_RECORD_SHORT.format(
      debtor.name, debtor.totalAmount, currency.name, constructListOfLogs(debtor.totalAmount, logs))
}

fun formatDebtorHistoryHeader(debtor: Debtor, currency: Currency): String {
  return DEBTOR_LOG_HISTORY_HEADER.format(debtor.name, debtor.totalAmount, currency.name)
}

fun formatDebtorHistoricalAmount(debtor: Debtor, logs: List<Log>, currency: Currency): String {
  return DEBTOR_HISTORICAL_CREDIT.format(
      debtor.name, calculateHistoricalCredit(logs), currency.name)
}

fun formatTotalAmountOfDebtsRecord(debtors: Set<Debtor>, currency: Currency): String {
  return CURRENT_DEBTS_TOTAL_AMOUNT.format(Debtor.totalAmount(debtors), currency.name)
}

fun formatMergedDebtorNotFound(source: String, destination: String, names: List<String>): String {
  return MERGE_DEBTOR_NOT_FOUND.format(source, destination, constructListOfDebtorNames(names))
}

fun formatMergedDebtorSuccess(count: Int, source: String, destination: String): String {
  return MERGE_DEBTOR_SUCCESS.format(count, source, destination)
}

fun formatShowMergedDebtorButton(name: String): String {
  return SHOW_MERGED_DEBTOR_BUTTON.format(name)
}

fun formatShowMergedCallback(name: String): String {
  return SHOW_MERGED_DEBTOR_CALLBACK.format(name)
}

fun formatDebtorSuggestionForInlineQuery(debtor: Debtor, currency: Currency): String {
  return DEBTOR_SUGGESTION_FOR_INLINE_QUERY.format(debtor.name, debtor.totalAmount, currency.name)
}

fun formatCurrencyCallback(name: String) = SET_CURRENCY_CALLBACK.format(name)

fun formatDeleteDebtorHistoryCallback(name: String) = DELETE_DEBTOR_HISTORY_CALLBACK.format(name)

fun formatDeleteDebtorHistoryWarningMessage(name: String) =
    DELETE_DEBTOR_HISTORY_WARNING.format(name)

fun formatCurrentCurrency(currency: Currency): String {
  return CURRENT_CURRENCY.format(currency.name)
}

fun formatSuccessfulAdminMergeMessage(
    chatId: Long,
    logCount: Int,
    destinationUser: String,
    sourceUser: String
): String {
  return ADMIN_MERGE_SUCCESS.format(chatId, logCount, destinationUser, sourceUser)
}

fun formatDeleteLastDebtorLogMessage(name: String, log: Log): String {
  return DELETE_LAST_DEBTOR_LOG.format(name, log.getAmount(), log.comment)
}

fun constructListOfAllDebtors(debtorsMap: Map<Debtor, List<Log>>, currency: Currency): String {
  val debtors = debtorsMap.keys
  val totalAmount = formatTotalAmountOfDebtsRecord(debtors, currency)
  val debtorsRows =
      debtors.sortedByDescending { it.totalAmount }.joinToString(separator = "\n") { debtor ->
        formatDebtorShortRecord(debtor, debtorsMap[debtor]!!, currency)
      }
  return totalAmount + debtorsRows
}

fun constructListOfDebtorNames(names: List<String>): String {
  return names.joinToString { debtor -> debtor }
}

fun constructListOfLogs(totalAmount: BigDecimal, logs: List<Log>): String {
  var amount = totalAmount
  return logs
      .sortedByDescending { it.created }
      .filter { log ->
        if (log.comment != REPAY_VALUE) amount -= log.credit
        (amount + log.credit) > BigDecimal.ZERO
      }
      .filter { it.comment != REPAY_VALUE }
      .joinToString(", ") { log -> log.comment }
}

fun constructDeleteDebtorMessageBasedOnDeletedCount(name: String, count: Int): String {
  return if (count > 0) SUCCESSFUL_DEBTOR_REMOVAL.format(name, count) else DEBTOR_NOT_FOUND
}

fun constructDeleteDebtorsMessageBasedOnDeletedCount(count: Int): String {
  return if (count > 0) SUCCESSFUL_DEBTORS_REMOVAL.format(count) else DEBTORS_NOT_FOUND
}

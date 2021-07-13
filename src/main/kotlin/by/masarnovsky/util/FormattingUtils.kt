package by.masarnovsky.util

import by.masarnovsky.*
import by.masarnovsky.Log.Companion.calculateHistoricalCredit
import java.math.BigDecimal

fun formatDebtorRecord(debtor: Debtor, logs: List<Log>): String {
    return DEBTOR_RECORD.format(debtor.name, debtor.totalAmount, constructListOfLogs(debtor.totalAmount, logs))
}

fun formatDebtorRecordForInlineQuery(debtor: Debtor, logs: List<Log>): String {
    return DEBTOR_RECORD_FOR_INLINE_QUERY.format(
        debtor.name,
        debtor.totalAmount,
        constructListOfLogs(debtor.totalAmount, logs)
    )
}

fun formatDebtorShortRecord(debtor: Debtor, logs: List<Log>): String {
    return DEBTOR_RECORD_SHORT.format(debtor.name, debtor.totalAmount, constructListOfLogs(debtor.totalAmount, logs))
}

fun formatDebtorHistoryHeader(debtor: Debtor): String {
    return DEBTOR_LOG_HISTORY_HEADER.format(debtor.name, debtor.totalAmount)
}

fun formatDebtorHistoricalAmount(debtor: Debtor, logs: List<Log>): String {
    return DEBTOR_HISTORICAL_CREDIT.format(debtor.name, calculateHistoricalCredit(logs))
}

fun formatTotalAmountOfDebtsRecord(debtors: Set<Debtor>): String {
    return CURRENT_DEBTS_TOTAL_AMOUNT.format(Debtor.totalAmount(debtors))
}

fun formatMergedDebtorNotFound(source: String, destination: String): String {
    return MERGE_DEBTOR_NOT_FOUND.format(source, destination)
}

fun formatMergedDebtorSuccess(count: Int, source: String, destination: String):String {
    return MERGE_DEBTOR_SUCCESS.format(count, source, destination)
}

fun constructListOfAllDebtors(debtorsMap: Map<Debtor, List<Log>>): String {
    val debtors = debtorsMap.keys
    val totalAmount = formatTotalAmountOfDebtsRecord(debtors)
    val debtorsRows =
        debtors.joinToString(separator = "\n") { debtor -> formatDebtorShortRecord(debtor, debtorsMap[debtor]!!) }
    return totalAmount + debtorsRows
}

fun constructListOfLogs(totalAmount: BigDecimal, logs: List<Log>): String {
    var amount = totalAmount
    return logs
        .sortedByDescending { it.created }
        .filter { log ->
            if (log.comment != REPAY_VALUE) amount -= log.credit //todo: do smth with it
            (amount + log.credit) > BigDecimal.ZERO
        }
        .filter { it.comment != REPAY_VALUE }
        .joinToString(", ") { log -> log.comment }
}

fun constructDeleteDebtorMessageBasedOnDeletedCount(name: String, count: Int): String {
    return if (count > 0) SUCCESSFUL_DEBTOR_REMOVAL.format(name) else DEBTOR_NOT_FOUND
}

fun constructDeleteDebtorsMessageBasedOnDeletedCount(count: Int): String {
    return if (count > 0) SUCCESSFUL_DEBTORS_REMOVAL.format(count) else DEBTORS_NOT_FOUND
}
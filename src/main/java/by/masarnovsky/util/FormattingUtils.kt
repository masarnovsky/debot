package by.masarnovsky.util

import by.masarnovsky.*
import java.math.BigDecimal

fun formatDebtorRecord(debtor: Debtor, logs: List<Log>): String {
    return DEBTOR_RECORD.format(debtor.name, debtor.totalAmount, constructListOfLogs(debtor.totalAmount, logs))
}

fun formatDebtorShortRecord(debtor: Debtor, logs: List<Log>): String {
    return DEBTOR_RECORD_SHORT.format(debtor.name, debtor.totalAmount, constructListOfLogs(debtor.totalAmount, logs))
}

fun formatTotalAmountOfDebtsRecord(debtors: Set<Debtor>): String {
    return ALL_DEBTS_TOTAL_AMOUNT.format(Debtor.totalAmount(debtors))
}

fun constructListOfAllDebtors(debtorsMap: Map<Debtor, List<Log>>): String {
    val debtors = debtorsMap.keys
    val totalAmount = formatTotalAmountOfDebtsRecord(debtors)
    val debtorsRows =
        debtors.joinToString(separator = "\n") { debtor -> formatDebtorShortRecord(debtor, debtorsMap[debtor]!!) }
    return totalAmount + debtorsRows
}

fun constructListOfLogs(totalAmount: BigDecimal, logs: List<Log>): String {
    return logs
        .sortedByDescending { it.created }
        .filter { log -> log.isEqualsToZeroAfterSubtractingFrom(totalAmount) }
        .filter { it.comment != REPAY_VALUE }
        .joinToString(", ") { log -> log.comment }
}

fun constructDeleteDebtorMessageBasedOnDeletedCount(name: String, count: Int): String {
    return if (count > 0) SUCCESSFUL_DEBTOR_REMOVAL.format(name) else DEBTOR_NOT_FOUND
}

fun constructDeleteDebtorsMessageBasedOnDeletedCount(count: Int): String {
    return if (count > 0) SUCCESSFUL_DEBTORS_REMOVAL.format(count) else DEBTORS_NOT_FOUND
}
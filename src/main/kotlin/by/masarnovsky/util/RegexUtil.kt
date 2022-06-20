package by.masarnovsky.util

import by.masarnovsky.*

fun isStringMatchDebtPattern(str: String): Boolean {
    return Regex(NEW_DEBTOR_PATTERN) matches str
}

fun isStringMatchRepayPattern(str: String): Boolean {
    return Regex(REPAY_PATTERN) matches str
}

fun isStringMatchMergePattern(str: String): Boolean {
    return Regex(MERGE_PATTERN) matches str
}

fun isStringMatchRevertPattern(str: String): Boolean {
    return Regex(REVERT_PATTERN) matches str
}

fun isStringMatchAdminMergeByDebtorIdPattern(str: String): Boolean {
    return Regex(ADMIN_MERGE_BY_DEBTOR_ID_PATTERN) matches str
}

fun isStringMatchShowMergePattern(str: String): Boolean {
    return Regex(SHOW_MERGED_PATTERN) matches str
}

fun isStringMatchSetCurrencyPattern(str: String): Boolean {
    return Regex(SET_CURRENCY_PATTERN) matches str
}

fun isStringMatchRevertLastLogPattern(str: String): Boolean {
    return Regex(REVERT_LAST_DEBTOR_LOG_PATTERN) matches str
}
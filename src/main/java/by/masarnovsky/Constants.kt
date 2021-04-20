package by.masarnovsky

const val PATTERN_NEW_DEBTOR = "(?<name>[\\p{L}\\s]*) (?<sum>[0-9.,]+) (?<comment>[\\p{L}\\s-!?)(.,]*)"
const val PATTERN_REPAY = "(?<name>[\\p{L}\\s]*) (?<sum>-[0-9.,]+)"
const val REPAY_VALUE = "Возврат суммы"

const val USERS_COLLECTION = "users"
const val DEBTS_COLLECTION = "debts"

const val DEBTORS_LIST_CALLBACK = "callback_list"
const val DELETE_HISTORY_CALLBACK = "delete_history_yes"
const val NOT_DELETE_HISTORY_CALLBACK = "delete_history_no"

const val START_COMMAND = "/start"
const val SHOW_COMMAND = "/show"
const val DELETE_COMMAND = "/delete"
const val ALL_COMMAND = "/all"
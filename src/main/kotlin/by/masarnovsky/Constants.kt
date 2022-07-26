package by.masarnovsky

const val POSTGRES_URL_PATTERN =
    "postgres:(?://(?:(?<username>[^:@]*)(?::(?<password>[^@]*))?@)?(?<addresses>(?:[a-zA-Z0-9\\-.]+|\\[[0-9a-f:]+])(?::(?:\\d+))?(?:,(?:[a-zA-Z0-9\\-.]+|\\[[0-9a-f:]+])(?::(?:\\d+))?)*)/)?(?<database>[^?&/]+)(?:[?&](?<parameters>.*))?"
const val NEW_DEBTOR_PATTERN =
    "(?<name>[\\p{L}\\s]*) (?<amount>[0-9.,]+) (?<comment>[\\p{L}\\s-!?)(.,]*)"
const val MERGE_PATTERN = "/merge (?<source>[\\p{L}\\s]*) (?<destination>[\\p{L}\\s]*)"
const val REVERT_PATTERN = "/revert (?<name>[\\p{L}\\s]*)"
const val ADMIN_MERGE_BY_DEBTOR_ID_PATTERN =
    "/amerge (?<chatId>[0-9]*) (?<source>[0-9]*) (?<destination>[0-9]*)"
const val REPAY_PATTERN = "(?<name>[\\p{L}\\s]*) (?<amount>-[0-9.,]+)"
const val SHOW_MERGED_PATTERN = "show_merged_(?<name>[\\p{L}\\s]*)"
const val DELETE_MERGED_PATTERN = "delete_merged_(?<name>[\\p{L}\\s]*)"
const val SET_CURRENCY_PATTERN = "set_currency_(?<currency>[A-Z]*)"
const val REVERT_LAST_DEBTOR_LOG_PATTERN = "revert_(?<debtorId>[0-9]*)_(?<logId>[0-9]*)"
const val DELETE_DEBTOR_HISTORY_PATTERN = "delete_debtor_history_(?<name>[\\p{L}\\s]*)_yes"

const val DEBTORS_LIST_CALLBACK = "callback_list"
const val DELETE_HISTORY_CALLBACK = "delete_history_yes"
const val NOT_DELETE_HISTORY_CALLBACK = "delete_history_no"
const val DELETE_DEBTOR_HISTORY_CALLBACK = "delete_debtor_history_%s_yes"
const val NOT_DELETE_DEBTOR_HISTORY_CALLBACK = "delete_debtor_history_no"
const val SHOW_MERGED_DEBTOR_CALLBACK = "show_merged_%s"
const val DELETE_MERGED_DEBTOR_CALLBACK = "delete_merged_%s"
const val SET_CURRENCY_CALLBACK = "set_currency_%s"
const val REVERT_LAST_DEBTOR_LOG_CALLBACK = "revert_%s_%s"

const val START_COMMAND = "/start"
const val SHOW_COMMAND = "/show"
const val DELETE_COMMAND = "/delete"
const val ALL_COMMAND = "/all"
const val HOWTO_COMMAND = "/howto"
const val MERGE_COMMAND = "/merge"
const val REVERT_COMMAND = "/revert"
const val MEME_COMMAND = "/meme"

const val ADMIN_DEBTOR_MERGE_COMMAND = "/amerge"

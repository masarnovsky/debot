package by.masarnovsky

const val POSTGRES_URL_PATTERN="postgres:(?://(?:(?<username>[^:@]*)(?::(?<password>[^@]*))?@)?(?<addresses>(?:[a-zA-Z0-9\\-.]+|\\[[0-9a-f:]+])(?::(?:\\d+))?(?:,(?:[a-zA-Z0-9\\-.]+|\\[[0-9a-f:]+])(?::(?:\\d+))?)*)/)?(?<database>[^?&/]+)(?:[?&](?<parameters>.*))?"
const val NEW_DEBTOR_PATTERN = "(?<name>[\\p{L}\\s]*) (?<amount>[0-9.,]+) (?<comment>[\\p{L}\\s-!?)(.,]*)"
const val MERGE_PATTERN = "/merge (?<source>[\\p{L}\\s]*) (?<destination>[\\p{L}\\s]*)"
const val REPAY_PATTERN = "(?<name>[\\p{L}\\s]*) (?<amount>-[0-9.,]+)"
const val SHOW_MERGED_PATTERN = "show_merged_(?<name>[\\p{L}\\s]*)"
const val SET_CURRENCY_PATTERN = "set_currency_(?<currency>[A-Z]*)"

const val DEBTORS_LIST_CALLBACK = "callback_list"
const val DELETE_HISTORY_CALLBACK = "delete_history_yes"
const val NOT_DELETE_HISTORY_CALLBACK = "delete_history_no"
const val SHOW_MERGED_DEBTOR_CALLBACK = "show_merged_%s"
const val SET_LANG_CALLBACK = "set_lang_%s"
const val SET_CURRENCY_CALLBACK = "set_currency_%s"

const val START_COMMAND = "/start"
const val SHOW_COMMAND = "/show"
const val DELETE_COMMAND = "/delete"
const val ALL_COMMAND = "/all"
const val HOWTO_COMMAND = "/howto"
const val MERGE_COMMAND = "/merge"

const val MIGRATE_USERS_COMMAND = "/migrateu"
const val MIGRATE_DEBTORS_COMMAND = "/migrated"
const val MEME_COMMAND = "/meme"

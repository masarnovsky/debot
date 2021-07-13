package by.masarnovsky

const val POSTGRES_URL_PATTERN="postgres:(?://(?:(?<username>[^:@]*)(?::(?<password>[^@]*))?@)?(?<addresses>(?:[a-zA-Z0-9\\-.]+|\\[[0-9a-f:]+])(?::(?:\\d+))?(?:,(?:[a-zA-Z0-9\\-.]+|\\[[0-9a-f:]+])(?::(?:\\d+))?)*)/)?(?<database>[^?&/]+)(?:[?&](?<parameters>.*))?"
const val PATTERN_NEW_DEBTOR = "(?<name>[\\p{L}\\s]*) (?<amount>[0-9.,]+) (?<comment>[\\p{L}\\s-!?)(.,]*)"
const val PATTERN_MERGE = "/merge (?<source>[\\p{L}\\s]*) (?<destination>[\\p{L}\\s]*)"
const val PATTERN_REPAY = "(?<name>[\\p{L}\\s]*) (?<amount>-[0-9.,]+)"

const val DEBTORS_LIST_CALLBACK = "callback_list"
const val DELETE_HISTORY_CALLBACK = "delete_history_yes"
const val NOT_DELETE_HISTORY_CALLBACK = "delete_history_no"

const val START_COMMAND = "/start"
const val SHOW_COMMAND = "/show"
const val DELETE_COMMAND = "/delete"
const val ALL_COMMAND = "/all"
const val HOWTO_COMMAND = "/howto"
const val MERGE_COMMAND = "/merge"

const val MIGRATE_USERS_COMMAND = "/migrateu"
const val MIGRATE_DEBTORS_COMMAND = "/migrated"

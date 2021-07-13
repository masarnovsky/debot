package by.masarnovsky.util

import by.masarnovsky.*
import com.elbekD.bot.types.InlineKeyboardButton
import com.elbekD.bot.types.InlineKeyboardMarkup
import com.elbekD.bot.types.InlineQueryResultArticle
import com.elbekD.bot.types.InputTextMessageContent
import java.util.*

fun createDeleteAllDebtorsKeyboard(): InlineKeyboardMarkup {
    val yes = InlineKeyboardButton(text = YES, callback_data = DELETE_HISTORY_CALLBACK)
    val no = InlineKeyboardButton(text = NO, callback_data = NOT_DELETE_HISTORY_CALLBACK)
    return InlineKeyboardMarkup(listOf(listOf(yes, no)))
}

fun createInlineQueryResultArticle(debtor: Debtor, logs: List<Log>): InlineQueryResultArticle {
    return InlineQueryResultArticle(
        id = UUID.randomUUID().toString(),
        title = debtor.name,
        input_message_content = createInputTextMessageContent(debtor, logs),
        description = DEBTOR_SUGGESTION_FOR_INLINE_QUERY.format(debtor.name, debtor.totalAmount),
    )
}

fun createInputTextMessageContent(debtor: Debtor, logs: List<Log>): InputTextMessageContent {
    return InputTextMessageContent(
        message_text = formatDebtorRecordForInlineQuery(debtor, logs),
        parse_mode = "HTML",
    )
}

fun createMainMenuKeyboard(): InlineKeyboardMarkup {
    val list = InlineKeyboardButton(text = LIST_OF_ALL, callback_data = DEBTORS_LIST_CALLBACK)
    return InlineKeyboardMarkup(listOf(listOf(list)))
}
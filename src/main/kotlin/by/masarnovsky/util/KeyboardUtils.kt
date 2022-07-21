package by.masarnovsky.util

import by.masarnovsky.*
import by.masarnovsky.Currency
import com.elbekD.bot.types.*
import java.util.*

fun createDeleteAllDebtorsKeyboard(): InlineKeyboardMarkup {
  val yes = InlineKeyboardButton(text = YES, callback_data = DELETE_HISTORY_CALLBACK)
  val no = InlineKeyboardButton(text = NO, callback_data = NOT_DELETE_HISTORY_CALLBACK)
  return InlineKeyboardMarkup(listOf(listOf(yes, no)))
}

fun createDeleteDebtorKeyboard(name: String): InlineKeyboardMarkup {
  val yes =
      InlineKeyboardButton(text = YES, callback_data = formatDeleteDebtorHistoryCallback(name))
  val no = InlineKeyboardButton(text = NO, callback_data = NOT_DELETE_DEBTOR_HISTORY_CALLBACK)
  return InlineKeyboardMarkup(listOf(listOf(yes, no)))
}

fun createInlineQueryResultArticle(
    debtor: Debtor,
    logs: List<Log>,
    currency: Currency
): InlineQueryResultArticle {
  return InlineQueryResultArticle(
      id = UUID.randomUUID().toString(),
      title = debtor.name,
      input_message_content = createInputTextMessageContent(debtor, logs, currency),
      description = formatDebtorSuggestionForInlineQuery(debtor, currency),
  )
}

fun createInlineQueryResultPhoto(url: String): InlineQueryResultPhoto {
  return InlineQueryResultPhoto(
      id = UUID.randomUUID().toString(),
      photo_url = url,
      thumb_url = url,
  )
}

fun createInputTextMessageContent(
    debtor: Debtor,
    logs: List<Log>,
    currency: Currency
): InputTextMessageContent {
  return InputTextMessageContent(
      message_text = formatDebtorRecordForInlineQuery(debtor, logs, currency),
      parse_mode = "HTML",
  )
}

fun createMainMenuKeyboard(): InlineKeyboardMarkup {
  val list =
      Currency.values().map {
        InlineKeyboardButton(text = it.name, callback_data = formatCurrencyCallback(it.name))
      }
  return InlineKeyboardMarkup(listOf(list))
}

fun createShowMergedUserKeyboard(name: String): InlineKeyboardMarkup {
  val list =
      InlineKeyboardButton(
          text = formatShowMergedDebtorButton(name), callback_data = formatShowMergedCallback(name))
  return InlineKeyboardMarkup(listOf(listOf(list)))
}

fun createDeleteLastDebtorLogKeyboard(debtorId: Long, logId: Long): InlineKeyboardMarkup {
  val yes =
      InlineKeyboardButton(
          text = YES, callback_data = REVERT_LAST_DEBTOR_LOG_CALLBACK.format(debtorId, logId))
  return InlineKeyboardMarkup(listOf(listOf(yes)))
}

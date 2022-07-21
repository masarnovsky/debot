package by.masarnovsky.service

import by.masarnovsky.bot
import com.elbekD.bot.types.InlineKeyboardMarkup
import com.elbekD.bot.types.ReplyKeyboard

const val PARSE_MODE_HTML = "HTML"

fun sendMessage(chatId: Long, text: String) {
  bot.sendMessage(chatId = chatId, text = text, parseMode = PARSE_MODE_HTML)
}

fun sendMessageWithKeyboard(chatId: Long, text: String, keyboard: ReplyKeyboard) {
  bot.sendMessage(chatId = chatId, text = text, markup = keyboard, parseMode = PARSE_MODE_HTML)
}

fun editMessageTextAndInlineKeyboard(
    chatId: Long,
    messageId: Int,
    text: String,
    keyboard: InlineKeyboardMarkup? = null
) {
  bot.editMessageReplyMarkup(chatId, messageId)
  bot.editMessageText(
      chatId = chatId,
      messageId = messageId,
      text = text,
      markup = keyboard,
      parseMode = PARSE_MODE_HTML)
}

fun sendImage(chatId: Long, imageUrl: String) {
  bot.sendPhoto(chatId = chatId, photo = imageUrl)
}

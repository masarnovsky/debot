package by.masarnovsky.service

import by.masarnovsky.bot
import com.elbekD.bot.types.InlineKeyboardMarkup
import com.elbekD.bot.types.ReplyKeyboard

fun sendMessage(chatId: Long, text: String) {
    bot.sendMessage(chatId = chatId, text = text, parseMode = "HTML")
}

fun sendMessageWithKeyboard(chatId: Long, text: String, keyboard: ReplyKeyboard) {
    bot.sendMessage(chatId = chatId, text = text, markup = keyboard, parseMode = "HTML")
}

fun editMessageTextAndInlineKeyboard(chatId: Long, messageId: Int, text: String, keyboard: InlineKeyboardMarkup?= null) {
    bot.editMessageReplyMarkup(chatId, messageId)
    bot.editMessageText(
        chatId = chatId,
        messageId = messageId,
        text = text,
        markup = keyboard
    )
}

fun sendImage(chatId: Long, imageUrl: String) {
    bot.sendPhoto(chatId = chatId, photo = imageUrl)
}
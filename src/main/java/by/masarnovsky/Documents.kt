package by.masarnovsky

import org.bson.types.ObjectId
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset

data class Debtor(
    var _id: ObjectId?,
    val chatId: Long,
    val name: String,
    var totalAmount: BigDecimal,
    var debts: MutableList<Debt>
) {
    constructor(chatId: Long, name: String, totalAmount: BigDecimal, debts: MutableList<Debt>) : this(
        null,
        chatId,
        name,
        totalAmount,
        debts
    )
}

data class Debt(val sum: BigDecimal, val comment: String, val date: LocalDateTime, var totalAmount: BigDecimal) {
    constructor(sum: BigDecimal, comment: String, date: LocalDateTime) : this(sum, comment, date, sum)
}

data class User(
    val _id: ObjectId?,
    val chatId: Long,
    var username: String?,
    var firstName: String?,
    var lastName: String?,
    var lastCommand: String?,
    var commandValue: String?,
    val created: LocalDateTime,
    var updated: LocalDateTime,
    var userId: Int?,
) {
    constructor(chatId: Long, username: String?, firstName: String?, lastName: String?, userId: Int?) : this(
        null,
        chatId,
        username,
        firstName,
        lastName,
        null,
        null,
        LocalDateTime.now(ZoneOffset.of("+03:00")),
        LocalDateTime.now(ZoneOffset.of("+03:00")),
        userId
    )
}
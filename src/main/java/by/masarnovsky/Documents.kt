package by.masarnovsky

import org.bson.types.ObjectId
import java.time.Instant

data class Debtor(
    var _id: ObjectId?,
    val chatId: Long,
    val name: String,
    var totalAmount: Double,
    var debts: MutableList<Debt>
) {
    constructor(chatId: Long, name: String, totalAmount: Double, debts: MutableList<Debt>) : this(
        null,
        chatId,
        name,
        totalAmount,
        debts
    )
}

data class Debt(val sum: Double, val comment: String, val date: Instant, var totalAmount: Double) {
    constructor(sum: Double, comment: String, date: Instant) : this(sum, comment, date, sum)
}
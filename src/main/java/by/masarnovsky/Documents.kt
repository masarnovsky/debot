package by.masarnovsky

import org.bson.types.ObjectId
import java.time.Instant

data class Debtor(val chatId: Long, val name: String, var totalAmount: Double, var debts: MutableList<Debt>) {
    constructor(chatId: Long, name: String, totalAmount: Double) : this(chatId, name, totalAmount, mutableListOf())

    var id: ObjectId? = null
}

data class Debt(val sum: Double, val comment: String, val date: Instant, val totalAmount: Double) {
    constructor(sum: Double, comment: String, date: Instant) : this(sum, comment, date, sum)
}
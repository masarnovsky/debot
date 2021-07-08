package by.masarnovsky

import by.masarnovsky.db.Debtors
import by.masarnovsky.db.Logs
import by.masarnovsky.db.Users
import by.masarnovsky.service.TimeService
import com.elbekD.bot.types.Message
import org.bson.types.ObjectId
import org.jetbrains.exposed.sql.ResultRow
import java.math.BigDecimal
import java.time.LocalDateTime

data class DebtorM(
    var _id: ObjectId?,
    val chatId: Long,
    val name: String,
    var totalAmount: BigDecimal,
    var debts: MutableList<DebtM>,
)

data class Debtor(
    var id: Long?,
    val userId: Long,
    val name: String,
    var totalAmount: BigDecimal = BigDecimal.ZERO,
    var created: LocalDateTime = TimeService.now(),
    var updated: LocalDateTime = TimeService.now(),
) {
    constructor(userId: Long, name: String, totalAmount: BigDecimal) : this(
        null,
        userId,
        name,
        totalAmount,
        TimeService.now(),
        TimeService.now(),
    )

    companion object {

        fun fromRow(resultRow: ResultRow) = Debtor(
            id = resultRow[Debtors.id].value,
            userId = resultRow[Debtors.userId].value,
            name = resultRow[Debtors.name],
            totalAmount = resultRow[Debtors.totalAmount],
            created = resultRow[Debtors.created],
            updated = resultRow[Debtors.updated],
        )
    }
}

data class DebtM(val sum: BigDecimal, val comment: String, val date: LocalDateTime, var totalAmount: BigDecimal)

data class Log(
    val id: Long?,
    val debtorId: Long,
    val credit: BigDecimal,
    val debit: BigDecimal,
    val created: LocalDateTime,
    val comment: String,
    val currency: String,
    val type: String,
) {
    constructor(debtorId: Long, credit: BigDecimal, debit: BigDecimal, comment: String) :
            this(
                null, debtorId, credit, debit,
                TimeService.now(),
                comment, "BYN", if (credit > BigDecimal.ZERO) "CREDIT" else "DEBIT"
            )

    fun isEqualsToZeroAfterSubtractingFrom(amount: BigDecimal): Boolean {
        var totalAmount = amount
        if (comment != REPAY_VALUE) totalAmount -= credit
        return totalAmount + credit > BigDecimal.ZERO
    }

    companion object {

        fun fromRow(resultRow: ResultRow) = Log(
            id = resultRow[Logs.id].value,
            debtorId = resultRow[Logs.debtorId].value,
            credit = resultRow[Logs.credit],
            debit = resultRow[Logs.debit],
            created = resultRow[Logs.created],
            comment = resultRow[Logs.comment],
            currency = resultRow[Logs.currency],
            type = resultRow[Logs.type]
        )
    }
}

data class User(
    val chatId: Long,
    var username: String?,
    var firstName: String?,
    var lastName: String?,
    val created: LocalDateTime = TimeService.now(),
    var updated: LocalDateTime = TimeService.now(),
    val defaultLang: String = "RU",
    val defaultCurrency: String = "BYN",
) {

    companion object {

        fun fromRow(resultRow: ResultRow) = User(
            chatId = resultRow[Users.id].value,
            username = resultRow[Users.username],
            firstName = resultRow[Users.firstName],
            lastName = resultRow[Users.lastName],
            created = resultRow[Users.created],
            updated = resultRow[Users.updated],
        )

        fun fromMessage(message: Message) = User(
            chatId = message.chat.id,
            username = message.chat.username,
            firstName = message.chat.first_name,
            lastName = message.chat.last_name
        )
    }
}
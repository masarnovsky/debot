package by.masarnovsky

import by.masarnovsky.db.Debtors
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
) {
    constructor(chatId: Long, name: String, totalAmount: BigDecimal, debts: MutableList<DebtM>) : this(
        null,
        chatId,
        name,
        totalAmount,
        debts,
    )
}

data class Debtor(
    var id: Long?,
    val userId: Long,
    val name: String,
    var totalAmount: BigDecimal,
    var created: LocalDateTime,
    var updated: LocalDateTime,
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

data class DebtM(val sum: BigDecimal, val comment: String, val date: LocalDateTime, var totalAmount: BigDecimal) {
    constructor(sum: BigDecimal, comment: String, date: LocalDateTime) : this(sum, comment, date, sum)
}

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
}

data class User(
    var id: Long?,
    val chatId: Long,
    var username: String?,
    var firstName: String?,
    var lastName: String?,
    val created: LocalDateTime,
    var updated: LocalDateTime,
    val defaultLang: String,
    val defaultCurrency: String,
) {
    constructor(
        chatId: Long, username: String?, firstName: String?,
        lastName: String?,
    ) : this(
        null,
        chatId,
        username,
        firstName,
        lastName,
        TimeService.now(),
        TimeService.now(),
        "RU",
        "BYN",
    )

    constructor(
        id: Long, chatId: Long, username: String?,
        firstName: String?, lastName: String?,
        created: LocalDateTime, updated: LocalDateTime,
    ) : this(
        id,
        chatId,
        username,
        firstName,
        lastName,
        created,
        updated,
        "RU",
        "BYN",
    )

    companion object {

        fun fromRow(resultRow: ResultRow) = User(
            id = resultRow[Users.id].value,
            chatId = resultRow[Users.chatId],
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

data class UserM(
    var _id: ObjectId?,
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
        TimeService.now(),
        TimeService.now(),
        userId
    )
}

fun UserM.copyInto(user: UserM?): UserM {
    return if (user != null) {
        user.firstName = this.firstName
        user.lastName = this.lastName
        user.username = this.username
        user.updated = TimeService.now()
        user
    } else {
        this
    }
}
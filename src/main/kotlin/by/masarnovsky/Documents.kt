package by.masarnovsky

import by.masarnovsky.db.Debtors
import by.masarnovsky.db.Images
import by.masarnovsky.db.Logs
import by.masarnovsky.db.Users
import by.masarnovsky.service.TimeService
import com.elbekD.bot.types.Message
import org.jetbrains.exposed.sql.ResultRow
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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

        fun totalAmount(debtors: Set<Debtor>): BigDecimal = debtors.sumOf { it.totalAmount }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Debtor

        if (userId != other.userId) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userId.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }
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

    constructor(debtorId: Long, credit: BigDecimal, debit: BigDecimal, comment: String, created: LocalDateTime) :
            this(
                null, debtorId, credit, debit,
                created,
                comment, "BYN", if (credit > BigDecimal.ZERO) "CREDIT" else "DEBIT"
            )

    fun summarize(): String {
        return LOG_SUMMARIZE.format(getCreatedDateAsString(), getAmount(), comment)
    }

    fun getAmountAsRawValue(): BigDecimal {
        return if (credit > BigDecimal.ZERO) credit
        else debit.multiply(BigDecimal(-1))
    }

    fun getAmount(): BigDecimal {
        return credit.max(debit)
    }

    private fun getCreatedDateAsString(): String {
        return created.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
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

        fun calculateHistoricalCredit(logs: List<Log>): BigDecimal {
            return logs
                .filter { it.type == "CREDIT" }
                .sumOf { it.credit }
        }
    }
}

data class User(
        val chatId: Long,
        var username: String?,
        var firstName: String?,
        var lastName: String?,
        val isBot: Boolean = false,
        val created: LocalDateTime = TimeService.now(),
        var updated: LocalDateTime = TimeService.now(),
        var defaultLang: String = "RU",
        var defaultCurrency: Currency,
) {

    companion object {

        fun fromRow(resultRow: ResultRow) = User(
            chatId = resultRow[Users.id].value,
            username = resultRow[Users.username],
            firstName = resultRow[Users.firstName],
            lastName = resultRow[Users.lastName],
            isBot = resultRow[Users.isBot],
            created = resultRow[Users.created],
            updated = resultRow[Users.updated],
            defaultCurrency = resultRow[Users.defaultCurrency],
        )

        fun fromMessage(message: Message) = User(
            chatId = message.chat.id,
            username = message.chat.username,
            firstName = message.chat.first_name,
            lastName = message.chat.last_name,
            defaultCurrency = Currency.BYN,
            isBot = message.from?.is_bot ?: false
        )
    }
}

data class Image(
        val id: Long,
        val url: String,
) {
    companion object {

        fun fromRow(resultRow: ResultRow) = Image(
                id = resultRow[Images.id].value,
                url = resultRow[Images.url],
        )
    }
}
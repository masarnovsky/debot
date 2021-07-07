package by.masarnovsky.db

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.`java-time`.datetime

object Users : LongIdTable() {
    val chatId = long("chat_id")
    val username = varchar("username", 200).nullable()
    val firstName = varchar("first_name", 200).nullable()
    val lastName = varchar("last_name", 200).nullable()
    val defaultLang = varchar("default_lang", 10)
    val defaultCurrency = varchar("default_currency", 10)
    val created = datetime("created")
    val updated = datetime("updated")
    override val primaryKey = PrimaryKey(id, name = "users_pkey")
}

object Debtors : LongIdTable() {
    val userId = reference("user_id", Users)
    val name = varchar("name", 200)
    val totalAmount = decimal("total_amount", 19, 2)
    val created = datetime("created")
    val updated = datetime("updated")
    override val primaryKey = PrimaryKey(id, name = "debtors_pk")
}

object Logs : LongIdTable() {
    val debtorId = reference("debtor_id", Debtors)
    val credit = decimal("credit", 19, 4)
    val debit = decimal("debit", 19, 4)
    val created = datetime("created")
    val comment = varchar("comment", 200)
    val currency = varchar("currency", 10)
    val type = varchar("type", 10)
    override val primaryKey = PrimaryKey(id, name = "logs_pk")
}

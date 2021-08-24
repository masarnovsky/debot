package by.masarnovsky.db

import by.masarnovsky.Currency
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.`java-time`.datetime

open class ChatIdTable(name: String = "", columnName: String = "id") : IdTable<Long>(name) {
    override val id: Column<EntityID<Long>> = long(columnName).entityId()
    override val primaryKey by lazy { super.primaryKey ?: PrimaryKey(id) }
}

object Users : ChatIdTable() {
    val username = varchar("username", 200).nullable()
    val firstName = varchar("first_name", 200).nullable()
    val lastName = varchar("last_name", 200).nullable()
    val defaultLang = varchar("default_lang", 10)
    val defaultCurrency = enumerationByName("default_currency", 10, Currency::class)
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
    val debtorId = reference("debtor_id", Debtors, ReferenceOption.CASCADE)
    val credit = decimal("credit", 19, 4)
    val debit = decimal("debit", 19, 4)
    val created = datetime("created")
    val comment = varchar("comment", 200)
    val currency = varchar("currency", 10)
    val type = varchar("type", 10)
    override val primaryKey = PrimaryKey(id, name = "logs_pk")
}

object Images : LongIdTable() {
    val url = varchar("url", 200)
    override val primaryKey = PrimaryKey(id, name = "images_pk")
}

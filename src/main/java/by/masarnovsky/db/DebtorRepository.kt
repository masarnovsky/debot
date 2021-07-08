package by.masarnovsky.db

import by.masarnovsky.Debtor
import by.masarnovsky.service.TimeService
import mu.KotlinLogging
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update

private val logger = KotlinLogging.logger {}

fun findDebtorByUserIdAndName(chatId: Long, name: String): Debtor? {
    logger.info { "find debtor with name $name for user $chatId" }
    return Debtors
        .select { (Debtors.userId eq chatId) and (Debtors.name eq name) }
        .firstOrNull()
        ?.let { Debtor.fromRow(it) }
}

fun insertDebtor(debtor: Debtor): Long {
    logger.info { "save new debtor $debtor for user ${debtor.userId}" }
    return Debtors.insertAndGetId {
        it[userId] = debtor.userId
        it[name] = debtor.name
        it[totalAmount] = debtor.totalAmount
        it[created] = debtor.created
        it[updated] = debtor.updated
    }.value
}

fun updateDebtor(debtor: Debtor) {
    logger.info { "update debtor $debtor" }
    Debtors.update({ Debtors.id eq debtor.id }) {
        it[userId] = debtor.userId
        it[name] = debtor.name
        it[totalAmount] = debtor.totalAmount
        it[created] = debtor.created
        it[updated] = TimeService.now()
    }
}

fun findDebtorsForUser(chatId: Long): List<Debtor> {
    logger.info { "find all debtor for user:$chatId" }
    return Debtors.select { Debtors.userId eq chatId }.map { Debtor.fromRow(it) }
}
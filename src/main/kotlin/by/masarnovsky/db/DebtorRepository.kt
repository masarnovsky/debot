package by.masarnovsky.db

import by.masarnovsky.Debtor
import by.masarnovsky.service.TimeService
import mu.KotlinLogging
import org.jetbrains.exposed.sql.*
import java.math.BigDecimal

private val logger = KotlinLogging.logger {}

fun findDebtorByUserIdAndName(chatId: Long, name: String): Debtor? {
    logger.info { "find debtor with name $name for user $chatId" }
    return Debtors
        .select { (Debtors.userId eq chatId) and (Debtors.name eq name.toLowerCase()) }
        .firstOrNull()
        ?.let { Debtor.fromRow(it) }
}

fun insertDebtor(debtor: Debtor): Long {
    logger.info { "save new debtor $debtor for user ${debtor.userId}" }
    return Debtors.insertAndGetId {
        it[userId] = debtor.userId
        it[name] = debtor.name.toLowerCase()
        it[totalAmount] = debtor.totalAmount
        it[created] = debtor.created
        it[updated] = debtor.updated
    }.value
}

fun updateDebtor(debtor: Debtor) {
    logger.info { "update debtor $debtor" }
    Debtors.update({ Debtors.id eq debtor.id }) {
        it[userId] = debtor.userId
        it[name] = debtor.name.toLowerCase()
        it[totalAmount] = debtor.totalAmount
        it[created] = debtor.created
        it[updated] = TimeService.now()
    }
}

fun findDebtorsForUser(chatId: Long): List<Debtor> {
    logger.info { "find all debtors for user:$chatId" }
    return Debtors.select { Debtors.userId eq chatId }.map { Debtor.fromRow(it) }
}

fun findDebtorsWithCreditForUser(chatId: Long): List<Debtor> {
    logger.info { "find debtors with credit for user:$chatId" }
    return Debtors.select { (Debtors.userId eq chatId) and (Debtors.totalAmount greater BigDecimal.ZERO) }.map { Debtor.fromRow(it) }
}

fun deleteAllDebtorsForUser(chatId: Long): Int {
    return Debtors.deleteWhere { Debtors.userId eq chatId }
}

fun deleteDebtorForUserByName(chatId: Long, name: String): Int {
    return Debtors.deleteWhere { (Debtors.userId eq chatId) and (Debtors.name eq name.toLowerCase()) }
}
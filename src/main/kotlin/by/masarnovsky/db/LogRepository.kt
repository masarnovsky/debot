package by.masarnovsky.db

import by.masarnovsky.Log
import mu.KotlinLogging
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select

private val logger = KotlinLogging.logger {}

fun insertLog(log: Log): Long {
    logger.info { "save new log $log" }
    return Logs.insertAndGetId {
        it[debtorId] = log.debtorId
        it[credit] = log.credit
        it[debit] = log.debit
        it[created] = log.created
        it[comment] = log.comment
        it[currency] = log.currency
        it[type] = log.type
    }.value
}

fun findLogsForDebtorByDebtorId(debtorId: Long): List<Log> {
    logger.info { "find logs for debtor:$debtorId" }
    return Logs
        .select { Logs.debtorId eq debtorId }
        .map { Log.fromRow(it) }
}
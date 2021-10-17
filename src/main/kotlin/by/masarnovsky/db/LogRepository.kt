package by.masarnovsky.db

import by.masarnovsky.Log
import mu.KotlinLogging
import org.jetbrains.exposed.sql.*

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
        .orderBy(Logs.created, SortOrder.ASC)
        .map { Log.fromRow(it) }
}

fun findLastLogForDebtorByDebtorId(debtorId: Long): Log? {
    logger.info { "find last log for debtor:$debtorId" }
    return Logs
        .select { Logs.debtorId eq debtorId }
            .orderBy(Logs.created, SortOrder.DESC)
            .limit(1)
            .map { Log.fromRow(it) }
            .firstOrNull()
}

fun findLogByIdAndDebtorId(id: Long, debtorId:Long): Log? {
    logger.info { "find log $id for debtor:$debtorId" }

    return Logs
        .select { (Logs.id eq id) and (Logs.debtorId eq debtorId)}
        .firstOrNull()
        ?.let { Log.fromRow(it) }
}

fun deleteLogById(id: Long): Int {
    return Logs.deleteWhere { (Logs.id eq id) }
}
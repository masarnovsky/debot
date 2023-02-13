package by.masarnovsky.db

import by.masarnovsky.databasePassword
import by.masarnovsky.databaseUrl
import by.masarnovsky.databaseUser
import mu.KotlinLogging
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

private val logger = KotlinLogging.logger {}

fun connection(): Database {
  return Database.connect(
      url = databaseUrl,
      driver = "org.postgresql.Driver",
      user = databaseUser,
      password = databasePassword,
  )
}

fun flywayMigration() {
  connection()
  logger.info { "start flyway migration" }
  val flyway = Flyway.configure().dataSource(databaseUrl, databaseUser, databasePassword).load()
  flyway.migrate()
}

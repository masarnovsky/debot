package by.masarnovsky.db

import by.masarnovsky.postgresPassword
import by.masarnovsky.postgresUrl
import by.masarnovsky.postgresUser
import org.jetbrains.exposed.sql.Database

fun connection(): Database {
  return Database.connect(
      url = postgresUrl,
      driver = "com.impossibl.postgres.jdbc.PGDriver",
      user = postgresUser,
      password = postgresPassword,
  )
}

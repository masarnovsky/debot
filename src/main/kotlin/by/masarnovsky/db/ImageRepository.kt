package by.masarnovsky.db

import by.masarnovsky.Image
import mu.KotlinLogging
import org.jetbrains.exposed.sql.selectAll

private val logger = KotlinLogging.logger {}

fun findAllImages(): List<Image> {
  logger.info { "find images" }
  return Images.selectAll().map { Image.fromRow(it) }
}

package by.masarnovsky.service

import java.time.LocalDateTime
import java.time.ZoneOffset

class TimeService {

  companion object {
    fun now(): LocalDateTime {
      return LocalDateTime.now(ZoneOffset.of("+03:00"))
    }
  }
}

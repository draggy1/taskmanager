package common

import java.time.LocalDateTime

trait WithStart {
  def getStart: LocalDateTime
  def isStartDateNotCorrect: Boolean
}

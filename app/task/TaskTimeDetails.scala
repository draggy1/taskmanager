package task

import java.time.LocalDateTime

case class TaskTimeDetails(start: LocalDateTime, end: LocalDateTime, duration: TaskDuration, delete: Option[LocalDateTime] = Option.empty)

object TaskTimeDetails {
  def getTaskEnd(start: LocalDateTime,  duration: TaskDuration): LocalDateTime = start.plusHours(duration.hoursValue).plusMinutes(duration.minutesValue)
}
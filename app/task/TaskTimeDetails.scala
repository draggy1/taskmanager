package task

import java.time.LocalDateTime

case class TaskTimeDetails(start: LocalDateTime, duration: TaskDuration, delete: Option[LocalDateTime] = Option.empty)

package common

import task.{TaskDuration, TaskTimeDetails}
import task.TaskDuration.TASK_DURATION_EMPTY
import task.TaskTimeDetails.getTaskEnd

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.util.Try

case object TimeUtils {
  val NIL_LOCAL_DATE_TIME: LocalDateTime = LocalDateTime.of(1900,1,1,0,0,0)

  def prepareTaskTimeDetails(startDateAsString: String, durationOpt: Option[String]) = {
    val startDate = mapToLocalDateTime(startDateAsString)
    val duration = mapToDuration(durationOpt)
    TaskTimeDetails(startDate, getTaskEnd(startDate, duration), duration)
  }

  def mapToLocalDateTime(startDate: String): LocalDateTime = {
    val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
    Try(LocalDateTime.parse(startDate, formatter)).toOption match {
      case None => NIL_LOCAL_DATE_TIME
      case Some(startDate) => startDate
    }
  }

  def mapToDuration(duration: Option[String]): TaskDuration =
    duration match {
      case Some(duration) => TaskDuration.createFromString(duration)
      case None => TASK_DURATION_EMPTY
    }
}

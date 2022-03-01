package task

import common.utils.StringUtils.SPACE

import scala.util.Try

case class TaskDuration(hoursValue: Int, minutesValue: Int)

case object TaskDuration {
  val MAX_MINUTES = 60
  val MAX_HOURS = 8

  val TASK_DURATION_EMPTY: TaskDuration = TaskDuration(0, 0)

  def createFromString(duration: String): TaskDuration = {
    val durationHoursPattern = "(1 hour)|(([1-9]+[0-9]*) (hours))".r
    val durationMinutesPattern = "(1 minute)|(([1-9]+[0-9]*) (minutes))".r

    val hours = durationHoursPattern.findFirstIn(duration) match {
      case Some(hours) => getNumberFromString(hours, MAX_HOURS)
      case None => 0
    }

    val minutes = durationMinutesPattern.findFirstIn(duration) match {
      case Some(minutes) => getNumberFromString(minutes, MAX_MINUTES)
      case None => 0
    }

    TaskDuration(hours, minutes)
  }

  private def getNumberFromString(hours: String, maxNumber: Int) = {
    Try(hours.split(SPACE)(0).toInt)
      .toOption
      .map(number => if(number <= maxNumber) number else 0)
      .getOrElse(0)
  }
}

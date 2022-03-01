package task.commands

import common.utils.TimeUtils.NIL_LOCAL_DATE_TIME
import common.{Command, WithStart, WithTaskTimeDetails}
import io.jvm.uuid.UUID
import task.TaskTimeDetails

import java.time.LocalDateTime

case class CreateTaskCommand(projectId: String, authorId: UUID, taskTimeDetails: TaskTimeDetails, volume: Option[Int],
                             comment: Option[String]) extends Command with WithTaskTimeDetails with WithStart{
  override def getAuthorId: UUID = authorId

  override def getProjectId: String = projectId

  override def isProjectIdBlank: Boolean = projectId.isBlank

  override def isAuthorIdBlank: Boolean = authorId.toString.isBlank

  override def getProjectToCheckIfExist: String = projectId

  override def getTimeDetails: TaskTimeDetails = taskTimeDetails

  override def isStartDateNotCorrect: Boolean = NIL_LOCAL_DATE_TIME.equals(taskTimeDetails.start)

  override def getStart: LocalDateTime = taskTimeDetails.start
}

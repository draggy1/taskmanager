package task.commands

import common.LocalDateTimeUtil.NIL_LOCAL_DATE_TIME
import common.{Command, WithStart, WithTaskTimeDetails}
import io.jvm.uuid.UUID
import task.TaskTimeDetails

case class CreateTaskCommand(projectId: String, authorId: UUID, taskTimeDetails: TaskTimeDetails, volume: Option[Int],
                             comment: Option[String]) extends Command with WithTaskTimeDetails with WithStart{
  override def getAuthorId: UUID = authorId

  override def getProjectId: String = projectId

  override def isProjectIdBlank: Boolean = projectId.isBlank

  override def isAuthorIdBlank: Boolean = authorId.toString.isBlank

  override def getProjectToCheckIfExist: String = projectId

  override def getTimeDetails: TaskTimeDetails = taskTimeDetails

  override def isStartDateNotCorrect: Boolean = NIL_LOCAL_DATE_TIME.equals(taskTimeDetails.start)
}

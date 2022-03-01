package task.commands

import common.utils.TimeUtils.NIL_LOCAL_DATE_TIME
import common.{Command, WithStart, WithTaskTimeDetails}
import common.utils.UUIDUtils.UUID_NIL
import io.jvm.uuid.UUID
import task.TaskTimeDetails

import java.time.LocalDateTime

case class UpdateTaskCommand(projectIdOld: String,
                             projectIdNew: String,
                             authorIdOld: UUID,
                             authorIdNew: UUID,
                             startDateOld: LocalDateTime,
                             taskTimeDetails: TaskTimeDetails,
                             volume: Option[Int],
                             comment: Option[String]) extends Command with WithTaskTimeDetails with WithStart{
  override def getAuthorId: UUID = authorIdOld

  override def getProjectId: String = projectIdNew

  override def isProjectIdBlank: Boolean = projectIdOld.isBlank || projectIdNew.isBlank

  override def getProjectToCheckIfExist: String = projectIdOld

  override def isAuthorIdBlank: Boolean = UUID_NIL.equals(authorIdOld) || UUID_NIL.equals(authorIdNew)

  override def getTimeDetails: TaskTimeDetails = taskTimeDetails

  override def isStartDateNotCorrect: Boolean = NIL_LOCAL_DATE_TIME.equals(taskTimeDetails.start) || NIL_LOCAL_DATE_TIME.equals(startDateOld)

  override def getStart: LocalDateTime = taskTimeDetails.start
}



package task.commands

import common.utils.TimeUtils.NIL_LOCAL_DATE_TIME
import common.{Command, WithStart}
import common.utils.UUIDUtils.UUID_NIL
import io.jvm.uuid.UUID

import java.time.LocalDateTime

case class DeleteTaskCommand(projectId: String, authorId: UUID, start: LocalDateTime) extends Command with WithStart{
  override def getAuthorId: UUID = authorId

  override def getProjectId: String = projectId

  override def isProjectIdBlank: Boolean = projectId.isBlank

  override def getProjectToCheckIfExist: String = projectId

  override def isAuthorIdBlank: Boolean = UUID_NIL.equals(authorId)

  override def isStartDateNotCorrect: Boolean = NIL_LOCAL_DATE_TIME.equals(start)

  override def getStart: LocalDateTime = start
}

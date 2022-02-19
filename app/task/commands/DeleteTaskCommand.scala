package task.commands

import common.Command
import common.UUIDUtils.UUID_NIL
import io.jvm.uuid.UUID

import java.time.LocalDateTime

case class DeleteTaskCommand(projectId: String, authorId: UUID, start: LocalDateTime) extends Command {
  override def getAuthorId: UUID = authorId

  override def getProjectId: String = projectId

  override def isProjectIdBlank: Boolean = projectId.isBlank

  override def getProjectToCheckIfExist: String = projectId

  override def isAuthorIdBlank: Boolean = UUID_NIL.equals(authorId)
}

package project.commands

import common.Command
import common.UUIDUtils.UUID_NIL
import io.jvm.uuid.UUID

case class CreateProjectCommand(authorId: UUID, projectId: String) extends Command {
  override def isProjectIdBlank: Boolean = projectId.isBlank

  override def getAuthorId: UUID = authorId

  override def getProjectId: String = projectId

  override def getProjectToCheckIfExist: String = projectId

  override def isAuthorIdBlank: Boolean = UUID_NIL.equals(authorId)
}
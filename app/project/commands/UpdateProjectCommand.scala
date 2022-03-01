package project.commands

import common.Command
import common.utils.UUIDUtils.UUID_NIL
import io.jvm.uuid.UUID

case class UpdateProjectCommand(authorId: UUID, projectIdOld: String, projectIdNew: String) extends Command {
  override def isProjectIdBlank: Boolean = projectIdOld.isBlank || projectIdNew.isBlank

  override def getAuthorId: UUID = authorId

  override def getProjectId: String = projectIdNew

  override def getProjectToCheckIfExist: String = projectIdOld

  override def isAuthorIdBlank: Boolean = UUID_NIL.equals(authorId)
}

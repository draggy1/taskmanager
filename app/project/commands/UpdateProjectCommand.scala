package project.commands

import common.Command
import io.jvm.uuid.UUID

case class UpdateProjectCommand(authorId: UUID, projectIdOld: String, projectIdNew: String) extends Command {
  override def checkIfProjectIdIsBlank(): Boolean = projectIdOld.isBlank || projectIdNew.isBlank

  override def getAuthorId: UUID = authorId

  override def getProjectId: String = projectIdNew

  override def getProjectToCheckIfExist: String = projectIdOld
}

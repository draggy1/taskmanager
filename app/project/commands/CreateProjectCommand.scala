package project.commands

import common.Command
import io.jvm.uuid.UUID

case class CreateProjectCommand(authorId: UUID, projectId: String) extends Command {
  override def checkIfProjectIdIsBlank(): Boolean = projectId.isBlank

  override def getAuthorId: UUID = authorId

  override def getProjectId: String = projectId

  override def getProjectToCheckIfExist: String = projectId
}
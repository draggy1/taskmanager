package task.commands

import common.Command
import io.jvm.uuid.UUID
import task.TaskTimeDetails

case class CreateTaskCommand(projectId: String, authorId: UUID, taskTimeDetails: TaskTimeDetails, volume: Option[Int],
                             comment: Option[String]) extends Command {
  override def getAuthorId: UUID = authorId

  override def getProjectId: String = projectId

  override def isProjectIdBlank: Boolean = projectId.isBlank

  override def isAuthorIdBlank: Boolean = authorId.toString.isBlank

  override def getProjectToCheckIfExist: String = projectId
}

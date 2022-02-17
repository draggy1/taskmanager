package project.commands
import common.Command
import io.jvm.uuid.UUID

case class DeleteProjectCommand(override val authorId: UUID, override val projectId: String)
  extends Command(projectId, authorId)

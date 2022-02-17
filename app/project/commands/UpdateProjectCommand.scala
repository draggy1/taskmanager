package project.commands

import common.Command
import io.jvm.uuid.UUID

case class UpdateProjectCommand(override val authorId: UUID, projectIdOld: String, projectIdNew: String)
  extends Command(projectIdNew, authorId)

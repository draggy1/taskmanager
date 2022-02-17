package project.commands

import common.Command
import io.jvm.uuid.UUID

import scala.language.postfixOps

case class CreateProjectCommand(override val authorId: UUID, override val projectId: String)
  extends Command(projectId: String, authorId: UUID)
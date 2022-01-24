package task.commands

import common.{Command, StartDate}
import io.jvm.uuid.UUID

import java.time.LocalDateTime

case class DeleteTaskCommand(override val projectId: String,
                             override val authorId: UUID,
                             start: LocalDateTime) extends Command(projectId, authorId: UUID) with StartDate {
  override def getStart: LocalDateTime = start
}

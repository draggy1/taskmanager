package task.commands

import io.jvm.uuid.UUID

import java.time.LocalDateTime

case class DeleteTaskCommand(projectId: String, authorId: UUID, start: LocalDateTime)

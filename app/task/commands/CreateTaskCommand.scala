package task.commands

import io.jvm.uuid.UUID
import task.TaskTimeDetails

case class CreateTaskCommand(projectId: String, authorId: UUID, taskTimeDetails: TaskTimeDetails, volume: Option[Int],
                             comment: Option[String])

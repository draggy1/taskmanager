package task.commands

import io.jvm.uuid.UUID
import task.TaskTimeDetails

import java.time.LocalDateTime

case class UpdateTaskCommand(projectIdOld: String,
                             projectIdNew: String,
                             authorIdOld: UUID,
                             authorIdNew: UUID,
                             startDateOld: LocalDateTime,
                             taskTimeDetails: TaskTimeDetails,
                             volume: Option[Int],
                             comment: Option[String])



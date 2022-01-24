package task.commands

import common.{Command, StartDate, WithTimeDetails}
import io.jvm.uuid.UUID
import task.{TaskDuration, TaskTimeDetails}

import java.time.LocalDateTime


case class CreateTaskCommand(override val projectId: String,
                             override val authorId: UUID,
                             taskTimeDetails: TaskTimeDetails,
                             volume: Option[Int],
                             comment: Option[String]) extends Command(projectId, authorId: UUID)
  with StartDate
  with WithTimeDetails {

  override def getStart: LocalDateTime = taskTimeDetails.start

  override def getProjectId: String = projectId

  override def getTimeDetails: TaskTimeDetails = taskTimeDetails

  override def getDuration: TaskDuration = taskTimeDetails.duration
}

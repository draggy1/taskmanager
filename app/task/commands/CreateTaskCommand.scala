package task.commands

import authentication.Error
import common.LocalDateTimeUtil.NIL_LOCAL_DATE_TIME
import common.StringUtils.EMPTY
import common.UUIDUtils.UUID_NIL
import io.jvm.uuid.UUID
import pdi.jwt.JwtClaim
import play.api.http.Status
import play.api.libs.functional.syntax.{toAlternativeOps, toFunctionalBuilderOps}
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json._
import task.TaskDuration.TASK_DURATION_EMPTY
import task.TaskTimeDetails.getTaskEnd
import task.{TaskDuration, TaskTimeDetails}

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.util.Try

case class CreateTaskCommand(projectId: String, authorId: UUID, taskTimeDetails: TaskTimeDetails, volume: Option[Int],
                             comment: Option[String])

object CreateTaskCommand {
  implicit def taskReads: Reads[CreateTaskCommand] = {
    (((JsPath \ "project_id").read[String] or Reads.pure(EMPTY)) and
      ((JsPath \ "author_id").read[UUID] or Reads.pure(UUID_NIL)) and
      ((JsPath \ "start_date").read[String] or Reads.pure(EMPTY)) and
      (JsPath \ "duration").readNullable[String] and
      (JsPath \ "volume").readNullable[Int] and
      (JsPath \ "comment").readNullable[String]
      )((project_id, author_id, start_date, duration, volume, comment) =>
        CreateTaskCommand(project_id, author_id, prepareTaskTimeDetails(start_date, duration), volume, comment))
  }

  private def prepareTaskTimeDetails(start_date: String, durationOpt: Option[String]) = {
    val startDate = mapToLocalDateTime(start_date)
    val duration = mapToDuration(durationOpt)
    TaskTimeDetails(startDate, getTaskEnd(startDate, duration), duration)
  }

  def mapToLocalDateTime(startDate: String): LocalDateTime = {
    val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
    Try(LocalDateTime.parse(startDate, formatter)).toOption match {
      case None => NIL_LOCAL_DATE_TIME
      case Some(startDate) => startDate
    }
  }

  def mapToDuration(duration: Option[String]): TaskDuration = {
    duration match {
      case Some(duration) => TaskDuration.createFromString(duration)
      case None => TASK_DURATION_EMPTY
    }
  }

  def mapJwtToCommand(claim: JwtClaim): Either[Error, CreateTaskCommand] = {
    val json: JsValue = Json.parse(claim.content)
    val result: JsResult[CreateTaskCommand] = Json.fromJson[CreateTaskCommand](json)

    mapEventualErrorToResult(result)
  }

  private def mapEventualErrorToResult(result: JsResult[CreateTaskCommand]): Either[Error, CreateTaskCommand] =
    result.asEither
      .left
      .map(pathWithErrors => new Error(getValidationError(pathWithErrors), Status.BAD_REQUEST))

  private def getValidationError(pathWithErrors: collection.Seq[(JsPath, collection.Seq[JsonValidationError])]) =
    pathWithErrors
      .flatMap(error => error._2)
      .head
      .message
}

package task.commands

import authentication.Error
import common.StringUtils.EMPTY
import pdi.jwt.JwtClaim
import play.api.http.Status
import play.api.libs.functional.syntax.{toAlternativeOps, toFunctionalBuilderOps}
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json._
import task.TaskDuration.TASK_DURATION_EMPTY
import task.{TaskDuration, TaskTimeDetails}

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

case class CreateTaskCommand(projectId: String, taskTimeDetails: TaskTimeDetails, volume: Option[Int], comment: Option[String])

object CreateTaskCommand {

  implicit def taskReads: Reads[CreateTaskCommand] = {
    (((JsPath \ "project_id").read[String] or Reads.pure(EMPTY)) and
      ((JsPath \ "start_date").read[String] or Reads.pure(EMPTY )) and
      (JsPath \ "duration").readNullable[String] and
      (JsPath \ "volume").readNullable[Int] and
      (JsPath \ "comment").readNullable[String]
      )((project_id, start_date, duration, volume, comment) => CreateTaskCommand(project_id,  TaskTimeDetails(mapToLocalDateTime(start_date), mapToDuration(duration)), volume, comment))
  }

  def mapToLocalDateTime(startDate: String): LocalDateTime = {
    val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
    LocalDateTime.parse(startDate, formatter)
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

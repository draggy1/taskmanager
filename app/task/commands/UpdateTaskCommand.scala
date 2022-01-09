package task.commands

import authentication.Error
import common.LocalDateTimeUtil.NIL_LOCAL_DATE_TIME
import common.StringUtils.EMPTY
import common.UUIDUtils.UUID_NIL
import io.jvm.uuid.UUID
import pdi.jwt.JwtClaim
import play.api.http.Status.BAD_REQUEST
import play.api.libs.functional.syntax.{toAlternativeOps, toFunctionalBuilderOps}
import play.api.libs.json.{JsPath, JsResult, JsValue, Json, JsonValidationError, Reads}
import task.TaskDuration.TASK_DURATION_EMPTY
import task.TaskTimeDetails.getTaskEnd
import task.{TaskDuration, TaskTimeDetails}

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.util.Try

case class UpdateTaskCommand(projectIdOld: String,
                             projectIdNew: String,
                             authorIdOld: UUID,
                             authorIdNew: UUID,
                             startDateOld: LocalDateTime,
                             taskTimeDetails: TaskTimeDetails,
                             volume: Option[Int],
                             comment: Option[String])

object UpdateTaskCommand {
  implicit def taskReads: Reads[UpdateTaskCommand] = {
    (((JsPath \ "project_id_old").read[String] or Reads.pure(EMPTY)) and
      ((JsPath \ "project_id_new").read[String] or Reads.pure(EMPTY)) and
      ((JsPath \ "author_id_old").read[UUID] or Reads.pure(UUID_NIL)) and
      ((JsPath \ "author_id_new").read[UUID] or Reads.pure(UUID_NIL)) and
      ((JsPath \ "start_date_old").read[String] or Reads.pure(EMPTY)) and
      ((JsPath \ "start_date_new").read[String] or Reads.pure(EMPTY)) and
      (JsPath \ "duration").readNullable[String] and
      (JsPath \ "volume").readNullable[Int] and
      (JsPath \ "comment").readNullable[String]
      )((projectIdOld, projectIdNew, authorIdOld, authorIdNew, startDateOld, startDateNew, duration, volume, comment) =>
      UpdateTaskCommand(projectIdOld,
                        projectIdNew,
                        authorIdOld,
                        authorIdNew,
                        mapToLocalDateTime(startDateOld),
                        prepareTaskTimeDetails(startDateNew, duration),
                        volume,
                        comment))
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

  def mapJwtToCommand(claim: JwtClaim): Either[Error, UpdateTaskCommand] = {
    val json: JsValue = Json.parse(claim.content)
    val result: JsResult[UpdateTaskCommand] = Json.fromJson[UpdateTaskCommand](json)

    mapEventualErrorToResult(result)
  }

  private def mapEventualErrorToResult(result: JsResult[UpdateTaskCommand]): Either[Error, UpdateTaskCommand] =
    result.asEither
      .left
      .map(pathWithErrors => new Error(getValidationError(pathWithErrors), BAD_REQUEST))

  private def getValidationError(pathWithErrors: collection.Seq[(JsPath, collection.Seq[JsonValidationError])]) =
    pathWithErrors
      .flatMap(error => error._2)
      .head
      .message
}



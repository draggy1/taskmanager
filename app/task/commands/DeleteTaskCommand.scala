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

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.util.Try

case class DeleteTaskCommand(projectId: String, authorId: UUID, start: LocalDateTime)

object DeleteTaskCommand {
  implicit def taskReads: Reads[DeleteTaskCommand] = {
    (((JsPath \ "project_id").read[String] or Reads.pure(EMPTY)) and
      ((JsPath \ "author_id").read[UUID] or Reads.pure(UUID_NIL)) and
      ((JsPath \ "start_date").read[String] or Reads.pure(EMPTY))
      )((project_id, author_id, start_date) => DeleteTaskCommand(project_id, author_id, mapToLocalDateTime(start_date)))
  }

  def mapJwtToCommand(jwtClaim: JwtClaim): Either[Error, DeleteTaskCommand] = {
    val json: JsValue = Json.parse(jwtClaim.content)
    val result: JsResult[DeleteTaskCommand] = Json.fromJson[DeleteTaskCommand](json)

    mapEventualErrorToResult(result)
  }

  private def mapEventualErrorToResult(result: JsResult[DeleteTaskCommand]): Either[Error, DeleteTaskCommand] =
    result.asEither
      .left
      .map(pathWithErrors => new Error(getValidationError(pathWithErrors), BAD_REQUEST))

  private def getValidationError(pathWithErrors: collection.Seq[(JsPath, collection.Seq[JsonValidationError])]) =
    pathWithErrors
      .flatMap(error => error._2)
      .head
      .message

  def mapToLocalDateTime(startDate: String): LocalDateTime = {
    val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
    Try(LocalDateTime.parse(startDate, formatter)).toOption match {
      case None => NIL_LOCAL_DATE_TIME
      case Some(startDate) => startDate
    }
  }
}

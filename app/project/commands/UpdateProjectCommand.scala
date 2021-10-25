package project.commands

import authentication.Error
import common.StringUtils.EMPTY
import common.UUIDUtils.UUID_NIL
import io.jvm.uuid.UUID
import pdi.jwt.JwtClaim
import play.api.http.Status
import play.api.libs.functional.syntax.{toAlternativeOps, toFunctionalBuilderOps}
import play.api.libs.json._

case class UpdateProjectCommand(authorId: UUID, projectIdOld: String, projectIdNew: String)

case object UpdateProjectCommand {
  implicit def projectReads: Reads[UpdateProjectCommand] =
    ((JsPath \ "author_id").read[UUID] or Reads.pure(UUID_NIL) and
      ((JsPath \ "project_id_old").read[String] or Reads.pure(EMPTY)) and
      ((JsPath \ "project_id_new").read[String] or Reads.pure(EMPTY))
      )((authorId, projectIdOld, projectIdNew) => UpdateProjectCommand(authorId, projectIdOld, projectIdNew))

  def mapJwtToCommand(claim: JwtClaim): Either[Error, UpdateProjectCommand] = {
    val json: JsValue = Json.parse(claim.content)
    val result: JsResult[UpdateProjectCommand] = Json.fromJson[UpdateProjectCommand](json)

    mapEventualErrorToResult(result)
  }

  private def mapEventualErrorToResult(result: JsResult[UpdateProjectCommand]): Either[Error, UpdateProjectCommand] =
    result.asEither
      .left
      .map(pathWithErrors => new Error(getValidationError(pathWithErrors), Status.BAD_REQUEST))

  private def getValidationError(pathWithErrors: collection.Seq[(JsPath, collection.Seq[JsonValidationError])]) =
    pathWithErrors
      .flatMap(error => error._2)
      .head
      .message
}

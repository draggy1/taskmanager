package project.commands

import authentication.Error
import common.StringUtils.EMPTY
import common.UUIDUtils.UUID_NIL
import io.jvm.uuid.UUID
import pdi.jwt.JwtClaim
import play.api.http.Status
import play.api.libs.functional.syntax.{toAlternativeOps, toFunctionalBuilderOps}
import play.api.libs.json._

import scala.language.postfixOps

case class CreateProjectCommand(authorId: UUID, projectId: String)

case object CreateProjectCommand {
  implicit def projectReads: Reads[CreateProjectCommand] =
    ((JsPath \ "author_id").read[UUID] or Reads.pure(UUID_NIL) and
    ((JsPath \ "project_id").read[String] or Reads.pure(EMPTY))
  )((author_id, project_id) => CreateProjectCommand(author_id, project_id))

  def mapJwtToCommand(claim: JwtClaim): Either[Error, CreateProjectCommand] = {
    val json: JsValue = Json.parse(claim.content)
    val result: JsResult[CreateProjectCommand] = Json.fromJson[CreateProjectCommand](json)

    mapEventualErrorToResult(result)
  }

  private def mapEventualErrorToResult(result: JsResult[CreateProjectCommand]): Either[Error, CreateProjectCommand] =
    result.asEither
      .left
      .map(pathWithErrors => new Error(getValidationError(pathWithErrors), Status.BAD_REQUEST))

  private def getValidationError(pathWithErrors: collection.Seq[(JsPath, collection.Seq[JsonValidationError])]) =
    pathWithErrors
      .flatMap(error => error._2)
      .head
      .message
}
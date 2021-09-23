package project.commands

import authentication.Error
import io.jvm.uuid.UUID
import pdi.jwt.JwtClaim
import play.api.http.Status
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._

case class CreateProjectCommand(userId: UUID, projectId: String)

case object CreateProjectCommand{
  implicit def projectReads: Reads[CreateProjectCommand] = (
    (JsPath \ "user_id").read[UUID](userIdValidator) and
      (JsPath \ "project_id").read[String](projectIdIsBlank)
    )((user_id, project_id) => CreateProjectCommand(user_id, project_id))

  val projectIdIsBlank: Reads[String] =
    Reads.StringReads.filterNot(JsonValidationError("Empty project id provided"))(projectId => projectId.isBlank)

  val userIdValidator: Reads[UUID] =
    Reads.uuidReads.filterNot(JsonValidationError("Problem with user id"))(uuid => uuid.toString.isBlank)

  def mapIfPossible(claim: JwtClaim): Either[Error, CreateProjectCommand] = {
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
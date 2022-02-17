package common
import authentication.Error
import pdi.jwt.JwtClaim
import play.api.http.Status
import play.api.libs.json._

object JwtUtils {
  def mapJwtToCommand[T](claim: JwtClaim)(implicit reads: Reads[T]): Either[Error, T] = {
    val json: JsValue = Json.parse(claim.content)
    val result: JsResult[T] = Json.fromJson[T](json)

    mapEventualErrorToResult(result)
  }

  private def mapEventualErrorToResult[T](result: JsResult[T]): Either[Error, T] =
    result.asEither
      .left
      .map(pathWithErrors => new Error(getValidationError(pathWithErrors), Status.BAD_REQUEST))

  private def getValidationError(pathWithErrors: collection.Seq[(JsPath, collection.Seq[JsonValidationError])]) =
    pathWithErrors
      .flatMap(error => error._2)
      .head
      .message
}

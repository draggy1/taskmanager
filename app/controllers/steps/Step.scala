package controllers.steps

import authentication.{AuthenticationHandler, Error}
import common.utils.JwtUtils
import pdi.jwt.JwtClaim
import play.api.libs.json.Reads
import play.api.mvc.{AnyContent, Request, Result}

import scala.concurrent.Future

abstract class Step[C](authHandler: AuthenticationHandler, reads: Reads[C]) {
  protected val authenticate: Request[AnyContent] => Either[Error, JwtClaim] = (request: Request[AnyContent]) => authHandler.performWithAuthentication(request)
  protected val mapToCommand: Either[Error, JwtClaim] => Either[Error, C] = {
    case Left(result) => Left(result)
    case Right(jwtClaims) => JwtUtils.mapJwtToCommand(jwtClaims)(reads)
  }
  def prepare(): Request[AnyContent] => Future[Either[Error, Future[Result]]]
}

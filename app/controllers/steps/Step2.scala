package controllers.steps

import authentication.{AuthenticationHandler, Error}
import pdi.jwt.JwtClaim
import play.api.mvc.{AnyContent, Request, Result}

import scala.concurrent.Future

abstract class Step2(authHandler: AuthenticationHandler) {
  protected val authenticate: Request[AnyContent] => Either[Error, JwtClaim] = (request: Request[AnyContent]) => authHandler.performWithAuthentication(request)
  def prepare(): Request[AnyContent] => Future[Either[Error, Future[Result]]]
}
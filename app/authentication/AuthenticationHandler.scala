package authentication

import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import play.api.Configuration
import play.api.http.HeaderNames
import play.api.mvc.{AnyContent, Request}

import javax.inject.Inject

case class AuthenticationHandler @Inject()(config: Configuration) {
  private def secretKey = config.get[String]("secret.key")
  def performWithAuthentication(request: Request[AnyContent]): Either[Error, JwtClaim] =
    extractToken
    .andThen(decodeJwtToken)
    .apply(request)

  private val extractToken = (request: Request[AnyContent]) =>
    request.headers.get(HeaderNames.AUTHORIZATION).map {
      case s"""Bearer $token""" => Right(token)
      case _ => Left(WithoutBearerToken)
    }.getOrElse(Left(WithoutHeader))

  private val decodeJwtToken = (token: Either[Error, String]) =>
    token match {
      case Left(result) => Left(result)
      case Right(token) => Jwt.decode(token, secretKey, Seq(JwtAlgorithm.HS256))
        .toEither
        .left
        .map(_=> IncorrectJwtToken)
    }
}



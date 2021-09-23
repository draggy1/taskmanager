package common

import authentication.Error
import play.api.http.Status
import play.api.mvc.Result
import play.api.mvc.Results.{BadRequest, InternalServerError, Unauthorized}

case object ResultMapper{
  def mapErrorToResult(error: Error): Result =
    error.statusToReturn match {
      case Status.BAD_REQUEST => BadRequest(error.message)
      case Status.UNAUTHORIZED => Unauthorized(error.message)
      case _=> InternalServerError(error.message)
    }
}

package authentication

import play.api.http.Status

sealed class Error(val message: String, val statusToReturn: Int)

case object WithoutHeader extends Error("Request not contains authorization header", Status.BAD_REQUEST)
case object WithoutBearerToken extends Error("Request not contains bearer token", Status.BAD_REQUEST)
case object IncorrectJwtToken extends Error("Could not decode JWT token", Status.UNAUTHORIZED)
case object DuplicatedProjectId extends Error("Provided project id already exist", Status.BAD_REQUEST)
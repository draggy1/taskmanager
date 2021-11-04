package authentication

import play.api.http.Status

sealed class Error(val message: String, val statusToReturn: Int)

case object WithoutHeader extends Error("Request not contains authorization header", Status.BAD_REQUEST)
case object WithoutBearerToken extends Error("Request not contains bearer token", Status.BAD_REQUEST)
case object IncorrectJwtToken extends Error("Could not decode JWT token", Status.UNAUTHORIZED)
case object DuplicatedProjectId extends Error("Provided project id already exist", Status.BAD_REQUEST)
case object EmptyProjectId extends Error("Provided project id is empty", Status.BAD_REQUEST)
case object EmptyAuthorId extends Error("Provided author id is not valid", Status.BAD_REQUEST)
case object NotValidUUID extends Error("Provided author id is not valid UUID", Status.BAD_REQUEST)
case object UserIsNotAuthor extends Error("Provided author is not author of the project", Status.BAD_REQUEST)
case object ProjectIdNotFound extends Error("Provided project could not be found", Status.BAD_REQUEST)
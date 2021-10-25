package controllers.actions

import authentication.{AuthenticationHandler, Error}
import pdi.jwt.JwtClaim
import play.api.mvc.{AnyContent, Request, Result}
import project.ProjectAggregate
import project.commands.UpdateProjectCommand
import project.validators.UpdateProjectValidator

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UpdateProjectActions (aggregate: ProjectAggregate,
                            authHandler: AuthenticationHandler) {

  def prepare(): Request[AnyContent] => Future[Either[Error, Future[Result]]] = {
    authenticate
      .andThen(mapToCommand)
      .andThen(validate)
      .andThen(performUpdateProject)
  }

  private val authenticate = (request: Request[AnyContent]) => authHandler.performWithAuthentication(request)

  private val mapToCommand = (result: Either[Error, JwtClaim]) =>
    result match {
      case Left(result) => Left(result)
      case Right(jwtClaims) => UpdateProjectCommand.mapJwtToCommand(jwtClaims)
    }

  private val validate:  Either[Error, UpdateProjectCommand] => Future[Either[Error, UpdateProjectCommand]] = {
    case Left(result) => Future.successful(Left(result))
    case Right(command) => UpdateProjectValidator(aggregate).validate(command)
  }

  private val performUpdateProject = (result: Future[Either[Error, UpdateProjectCommand]]) =>
    result.map {
      case Left(result) => Left(result)
      case Right(command) => Right(aggregate.updateProject(command))
    }
}

case object UpdateProjectActions {
  def apply(aggregate: ProjectAggregate, authHandler: AuthenticationHandler): UpdateProjectActions = new UpdateProjectActions(aggregate, authHandler)
}

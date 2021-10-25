package controllers.actions

import authentication.{AuthenticationHandler, Error}
import pdi.jwt.JwtClaim
import play.api.mvc.{AnyContent, Request, Result}
import project.ProjectAggregate
import project.commands.CreateProjectCommand
import project.validators.CreateProjectValidator

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CreateProjectActions (aggregate: ProjectAggregate,
                            authHandler: AuthenticationHandler){
  def prepare(): Request[AnyContent] => Future[Either[Error, Future[Result]]] = {
    authenticate
      .andThen(mapToCommand)
      .andThen(validate)
      .andThen(performCreationProject)
  }

  private val authenticate = (request: Request[AnyContent]) => authHandler.performWithAuthentication(request)

  private val mapToCommand = (result: Either[Error, JwtClaim]) =>
    result match {
      case Left(result) => Left(result)
      case Right(jwtClaims) => CreateProjectCommand.mapJwtToCommand(jwtClaims)
    }

  private val validate:  Either[Error, CreateProjectCommand] => Future[Either[Error, CreateProjectCommand]] = {
    case Left(result) => Future.successful(Left(result))
    case Right(command) => CreateProjectValidator(aggregate).validate(command)
  }

  private val performCreationProject = (result: Future[Either[Error, CreateProjectCommand]]) =>
    result.map {
      case Left(result) => Left(result)
      case Right(command) => Right(aggregate.createProject(command))
    }
}

case object CreateProjectActions {
  def apply(aggregate: ProjectAggregate, authHandler: AuthenticationHandler): CreateProjectActions = new CreateProjectActions(aggregate, authHandler)
}

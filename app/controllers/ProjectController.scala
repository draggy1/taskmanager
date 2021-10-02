package controllers

import authentication.{AuthenticationHandler, Error}
import common.ResultMapper.mapErrorToResult
import pdi.jwt.JwtClaim
import play.api.mvc._
import project.commands.CreateProjectCommand
import project.{ProjectAggregate, ProjectValidator}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ProjectController @Inject()(val controllerComponents: ControllerComponents,
                                  aggregate: ProjectAggregate,
                                  authHandler: AuthenticationHandler,
                                  validator: ProjectValidator) extends BaseController {
  /**
   * Create an endpoint for setting up a new project
   *
   * @return
   */
  def createProject(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authenticate
      .andThen(mapToCommand)
      .andThen(validate)
      .andThen(performCreationProject)
      .apply(request)
      .flatMap {
        case Left(error) => Future.successful(mapErrorToResult(error))
        case Right(result) => result
      }
  }

  private val authenticate = (request: Request[AnyContent]) => authHandler.performWithAuthentication(request)

  private val mapToCommand = (result: Either[Error, JwtClaim]) =>
    result match {
      case Left(result) => Left(result)
      case Right(jwtClaims) => CreateProjectCommand.mapIfPossible(jwtClaims)
    }

  private val validate:  Either[Error, CreateProjectCommand] => Future[Either[Error, CreateProjectCommand]] = {
    case Left(result) => Future.successful(Left(result))
    case Right(command) => validator.validate(command)
  }

  private val performCreationProject = (result: Future[Either[Error, CreateProjectCommand]]) =>
     result.map {
      case Left(result) => Left(result)
      case Right(command) => Right(aggregate.createProject(command))
  }
}


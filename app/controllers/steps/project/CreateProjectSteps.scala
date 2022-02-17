package controllers.steps.project

import authentication.{AuthenticationHandler, Error}
import controllers.steps.Step
import play.api.mvc.{AnyContent, Request, Result}
import project.ProjectAggregate
import project.ProjectReads.createProjectCommandReads
import project.commands.CreateProjectCommand
import project.validators.CreateProjectValidator

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CreateProjectSteps(projectAggregate: ProjectAggregate,
                         authHandler: AuthenticationHandler) extends Step(authHandler, createProjectCommandReads){
  def prepare(): Request[AnyContent] => Future[Either[Error, Future[Result]]] = {
    authenticate
      .andThen(mapToCommand)
      .andThen(validate)
      .andThen(performCreationProject)
  }

  private val validate:  Either[Error, CreateProjectCommand] => Future[Either[Error, CreateProjectCommand]] = {
    case Left(result) => Future.successful(Left(result))
    case Right(command) => CreateProjectValidator(projectAggregate).validate(command)
  }

  private val performCreationProject = (result: Future[Either[Error, CreateProjectCommand]]) =>
    result.map {
      case Left(result) => Left(result)
      case Right(command) => Right(projectAggregate.createProject(command))
    }
}

case object CreateProjectSteps {
  def apply(aggregate: ProjectAggregate, authHandler: AuthenticationHandler): CreateProjectSteps = new CreateProjectSteps(aggregate, authHandler)
}

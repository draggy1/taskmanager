package controllers.steps.project

import authentication.{AuthenticationHandler, Error}
import controllers.steps.Step
import play.api.mvc.{AnyContent, Request, Result}
import project.ProjectAggregate
import project.ProjectReads.updateProjectCommandReads
import project.commands.UpdateProjectCommand
import project.validators.UpdateProjectValidator
import task.TaskAggregate

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UpdateProjectSteps(projectAggregate: ProjectAggregate,
                         taskAggregate: TaskAggregate,
                         authHandler: AuthenticationHandler) extends Step(authHandler, updateProjectCommandReads){

  def prepare(): Request[AnyContent] => Future[Either[Error, Future[Result]]] =
    authenticate
    .andThen(mapToCommand)
    .andThen(validate)
    .andThen(performUpdateProject)

  private val validate:  Either[Error, UpdateProjectCommand] => Future[Either[Error, UpdateProjectCommand]] = {
    case Left(result) => Future.successful(Left(result))
    case Right(command) => UpdateProjectValidator(projectAggregate, taskAggregate).validate(command)
  }

  private val performUpdateProject = (result: Future[Either[Error, UpdateProjectCommand]]) =>
    result.map {
      case Left(result) => Left(result)
      case Right(command) => Right(projectAggregate.updateProject(command))
    }
}

case object UpdateProjectSteps {
  def apply(projectAggregate: ProjectAggregate, taskAggregate: TaskAggregate, authHandler: AuthenticationHandler): UpdateProjectSteps =
    new UpdateProjectSteps(projectAggregate, taskAggregate, authHandler)
}

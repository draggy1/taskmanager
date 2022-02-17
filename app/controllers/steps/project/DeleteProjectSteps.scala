package controllers.steps.project

import authentication.{AuthenticationHandler, Error}
import controllers.steps.Step
import play.api.mvc.{AnyContent, Request, Result}
import project.ProjectAggregate
import project.ProjectReads.deleteProjectCommandReads
import project.commands.DeleteProjectCommand
import project.validators.DeleteProjectValidator

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeleteProjectSteps(projectAggregate: ProjectAggregate,
                          authHandler: AuthenticationHandler) extends Step(authHandler, deleteProjectCommandReads) {
  override def prepare(): Request[AnyContent] => Future[Either[authentication.Error, Future[Result]]] = {
    authenticate
      .andThen(mapToCommand)
      .andThen(validate)
      .andThen(perform)
  }

  private val validate:  Either[Error, DeleteProjectCommand] => Future[Either[Error, DeleteProjectCommand]] = {
    case Left(result) => Future.successful(Left(result))
    case Right(command) => DeleteProjectValidator(projectAggregate).validate(command)
  }

  private val perform = (result: Future[Either[Error, DeleteProjectCommand]]) =>
    result.map {
      case Left(result) => Left(result)
      case Right(command) => Right(projectAggregate.delete(command))
    }
}

case object DeleteProjectSteps {
  def apply(projectAggregate: ProjectAggregate, authHandler: AuthenticationHandler): DeleteProjectSteps = {
    new DeleteProjectSteps(projectAggregate, authHandler)
  }
}

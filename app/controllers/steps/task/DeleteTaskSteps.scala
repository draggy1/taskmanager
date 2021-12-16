package controllers.steps.task

import authentication.{AuthenticationHandler, Error}
import controllers.steps.Step
import pdi.jwt.JwtClaim
import play.api.mvc.{AnyContent, Request, Result}
import project.ProjectAggregate
import task.TaskAggregate
import task.commands.DeleteTaskCommand
import task.validators.DeleteTaskValidator

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeleteTaskSteps (taskAggregate: TaskAggregate,
                       projectAggregate: ProjectAggregate,
                       authHandler: AuthenticationHandler) extends Step(authHandler){

  override def prepare(): Request[AnyContent] => Future[Either[Error, Future[Result]]] = {
    authenticate
      .andThen(mapToCommand)
      .andThen(validate)
      .andThen(performCreationTask)
  }

  private val mapToCommand = (result: Either[Error, JwtClaim]) =>
    result match {
      case Left(result) => Left(result)
      case Right(jwtClaims) => DeleteTaskCommand.mapJwtToCommand(jwtClaims)
    }

  private val validate:  Either[Error, DeleteTaskCommand] => Future[Either[Error, DeleteTaskCommand]] = {
    case Left(result) => Future.successful(Left(result))
    case Right(command) => DeleteTaskValidator(taskAggregate, projectAggregate).validate(command)
  }

  private val performCreationTask = (result: Future[Either[Error, DeleteTaskCommand]]) =>
    result.map {
      case Left(result) => Left(result)
      case Right(command) => Right(taskAggregate.deleteTask(command))
    }


}

object DeleteTaskSteps {
  def apply(aggregate: TaskAggregate, projectAggregate: ProjectAggregate, authHandler: AuthenticationHandler): DeleteTaskSteps =
    new DeleteTaskSteps(aggregate, projectAggregate, authHandler)
}

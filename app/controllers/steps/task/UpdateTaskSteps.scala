package controllers.steps.task

import authentication.{AuthenticationHandler, Error}
import controllers.steps.Step
import play.api.mvc.{AnyContent, Request, Result}
import project.ProjectAggregate
import task.TaskAggregate
import task.TaskReads.updateTaskCommandReads
import task.commands.UpdateTaskCommand
import task.validators.UpdateTaskValidator

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UpdateTaskSteps (taskAggregate: TaskAggregate,
                       projectAggregate: ProjectAggregate,
                       authHandler: AuthenticationHandler) extends Step(authHandler, updateTaskCommandReads){

  override def prepare(): Request[AnyContent] => Future[Either[Error, Future[Result]]] =
    authenticate
    .andThen(mapToCommand)
    .andThen(validate)
    .andThen(perform)

  private val validate:  Either[Error, UpdateTaskCommand] => Future[Either[Error, UpdateTaskCommand]] = {
    case Left(result) => Future.successful(Left(result))
    case Right(command) => UpdateTaskValidator(taskAggregate, projectAggregate).validate(command)
  }

  private val perform = (result: Future[Either[Error, UpdateTaskCommand]]) =>
    result.map {
      case Left(result) => Left(result)
      case Right(command) => Right(taskAggregate.updateTask(command))
    }
}

object UpdateTaskSteps {
  def apply(aggregate: TaskAggregate, projectAggregate: ProjectAggregate, authHandler: AuthenticationHandler): UpdateTaskSteps =
    new UpdateTaskSteps(aggregate, projectAggregate, authHandler)
}

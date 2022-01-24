package controllers.steps.task

import authentication.{AuthenticationHandler, Error}
import controllers.steps.Step
import play.api.mvc.{AnyContent, Request, Result}
import project.ProjectAggregate
import task.TaskAggregate
import task.TaskReads.createTaskCommandReads
import task.commands.CreateTaskCommand
import task.validators.CreateTaskValidator

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CreateTaskSteps(taskAggregate: TaskAggregate,
                      projectAggregate: ProjectAggregate,
                      authHandler: AuthenticationHandler) extends Step(authHandler, createTaskCommandReads){

  override def prepare(): Request[AnyContent] => Future[Either[Error, Future[Result]]] =
    authenticate
      .andThen(mapToCommand)
      .andThen(validate)
      .andThen(perform)


  private val validate:  Either[Error, CreateTaskCommand] => Future[Either[Error, CreateTaskCommand]] = {
    case Left(result) => Future.successful(Left(result))
    case Right(command) => CreateTaskValidator(taskAggregate, projectAggregate).validate(command)
  }

  private val perform = (result: Future[Either[Error, CreateTaskCommand]]) =>
    result.map {
      case Left(result) => Left(result)
      case Right(command) => Right(taskAggregate.createTask(command))
    }
}

object CreateTaskSteps {
  def apply(aggregate: TaskAggregate, projectAggregate: ProjectAggregate, authHandler: AuthenticationHandler): CreateTaskSteps =
    new CreateTaskSteps(aggregate, projectAggregate, authHandler)
}

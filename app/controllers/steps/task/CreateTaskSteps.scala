package controllers.steps.task

import authentication.{AuthenticationHandler, Error}
import controllers.steps.Step
import pdi.jwt.JwtClaim
import play.api.mvc.{AnyContent, Request, Result}
import task.TaskAggregate
import task.commands.CreateTaskCommand
import task.validators.CreateTaskValidator

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CreateTaskSteps(aggregate: TaskAggregate,
                      authHandler: AuthenticationHandler) extends Step(authHandler){
  override def prepare(): Request[AnyContent] => Future[Either[authentication.Error, Future[Result]]] =
    authenticate
      .andThen(mapToCommand)
      .andThen(validate)
      .andThen(performCreationTask)

  private val mapToCommand = (result: Either[Error, JwtClaim]) =>
    result match {
      case Left(result) => Left(result)
      case Right(jwtClaims) => CreateTaskCommand.mapJwtToCommand(jwtClaims)
    }

  private val validate:  Either[Error, CreateTaskCommand] => Future[Either[Error, CreateTaskCommand]] = {
    case Left(result) => Future.successful(Left(result))
    case Right(command) => CreateTaskValidator(aggregate).validate(command)
  }

  private val performCreationTask = (result: Future[Either[Error, CreateTaskCommand]]) =>
    result.map {
      case Left(result) => Left(result)
      case Right(command) => Right(aggregate.createTask(command))
    }
}

object CreateTaskSteps {
  def apply(aggregate: TaskAggregate, authHandler: AuthenticationHandler): CreateTaskSteps = new CreateTaskSteps(aggregate, authHandler)
}

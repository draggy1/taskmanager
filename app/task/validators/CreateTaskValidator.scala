package task.validators

import authentication.Error
import task.TaskAggregate
import task.commands.CreateTaskCommand

import scala.concurrent.Future

class CreateTaskValidator(aggregate: TaskAggregate) {
  def validate(command: CreateTaskCommand): Future[Either[Error, CreateTaskCommand]] = Future.successful(Right(command))

  /*val isProjectEmpty: CreateTaskCommand => Either[Error, CreateTaskCommand] =
    (command: CreateTaskCommand) => {
      if (command.projectId.isBlank) Left(EmptyProjectId) else Right(command)
    }

  val notValidAuthorId: Either[Error, CreateProjectCommand] => Either[Error, CreateProjectCommand] = {
    case Left(error) => Left(error)
    case Right(command) => if (UUID_NIL.equals(command.authorId)) Left(EmptyAuthorId) else Right(command)
  }

  val isDuplicated: Either[Error, CreateProjectCommand] => Future[Either[Error, CreateProjectCommand]] = {
    case Left(error) => Future.successful(Left(error))
    case Right(command) => isDuplicated(command)
  }

  private def isDuplicated(command: CreateProjectCommand) =
    aggregate.getProject(GetProjectByIdQuery(command.projectId))
      .map {
        case Some(_) => Left(DuplicatedProjectId)
        case None => Right(command)
      }*/
}

object CreateTaskValidator {
  def apply(aggregate: TaskAggregate): CreateTaskValidator = new CreateTaskValidator(aggregate)
}

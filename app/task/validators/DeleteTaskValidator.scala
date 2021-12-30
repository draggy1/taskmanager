package task.validators

import authentication.{EmptyAuthorId, EmptyProjectId, Error, IncorrectDate, TaskToDeleteNotExist, UserIsNotAuthor}
import common.LocalDateTimeUtil.NIL_LOCAL_DATE_TIME
import common.UUIDUtils.UUID_NIL
import project.ProjectAggregate
import project.queries.GetProjectByIdAndAuthorIdQuery
import task.TaskAggregate
import task.commands.DeleteTaskCommand
import task.queries.GetTaskByProjectIdAndStartQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeleteTaskValidator(taskAggregate: TaskAggregate, projectAggregate: ProjectAggregate) {

  def validate(command: DeleteTaskCommand): Future[Either[Error, DeleteTaskCommand]] =
    isProjectEmpty
      .andThen(notValidAuthorId)
      .andThen(isProperStartDate)
      .andThen(isProvidedTaskExist)
      .andThen(userIsNotAuthor)
      .apply(command)

  val isProjectEmpty: DeleteTaskCommand => Either[Error, DeleteTaskCommand] =
    (command: DeleteTaskCommand) => {
      if (command.projectId.isBlank) Left(EmptyProjectId) else Right(command)
    }

  val notValidAuthorId: Either[Error, DeleteTaskCommand] => Either[Error, DeleteTaskCommand] = {
    case Left(error) => Left(error)
    case Right(command) => if (UUID_NIL.equals(command.authorId)) Left(EmptyAuthorId) else Right(command)
  }

  val isProperStartDate: Either[Error, DeleteTaskCommand] => Either[Error, DeleteTaskCommand] = {
    case Left(error) => Left(error)
    case Right(command: DeleteTaskCommand) => if(NIL_LOCAL_DATE_TIME.equals(command.start))
      Left(IncorrectDate) else Right(command)
  }

  val isProvidedTaskExist: Either[Error, DeleteTaskCommand] => Future[Either[Error, DeleteTaskCommand]] = {
    case Left(error) => Future.successful(Left(error))
    case Right(command: DeleteTaskCommand) => isProvidedTaskExist(command)
  }

  private def isProvidedTaskExist(command: DeleteTaskCommand): Future[Either[Error, DeleteTaskCommand]] = {
    taskAggregate.getTask(GetTaskByProjectIdAndStartQuery(command.projectId, command.start))
      .map {
        case None => Left(TaskToDeleteNotExist)
        case Some(_) => Right(command)
      }
  }

  val userIsNotAuthor: Future[Either[Error, DeleteTaskCommand]] => Future[Either[Error, DeleteTaskCommand]] =
    (result: Future[Either[Error, DeleteTaskCommand]]) => {
      result.flatMap{
        case Left(error) => Future.successful(Left(error))
        case Right(command) => isUserAuthorOfProject(command)
      }
    }

  private def isUserAuthorOfProject(command: DeleteTaskCommand): Future[Either[Error, DeleteTaskCommand]] = {
    val query = GetProjectByIdAndAuthorIdQuery(command.projectId, command.authorId)
    projectAggregate.getProject(query)
      .map {
        case None => Left(UserIsNotAuthor)
        case Some(_) => Right(command)
      }
  }
}
object DeleteTaskValidator {
  def apply(taskAggregate: TaskAggregate, projectAggregate: ProjectAggregate): DeleteTaskValidator =
    new DeleteTaskValidator(taskAggregate, projectAggregate)
}

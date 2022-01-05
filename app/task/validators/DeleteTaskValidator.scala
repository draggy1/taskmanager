package task.validators

import authentication.{EmptyAuthorId, EmptyProjectId, Error, IncorrectDate, TaskToDeleteAlreadyDeleted, TaskToDeleteNotExist, UserIsNotAuthor}
import common.LocalDateTimeUtil.NIL_LOCAL_DATE_TIME
import common.UUIDUtils.UUID_NIL
import project.ProjectAggregate
import project.queries.GetProjectByIdAndAuthorIdQuery
import task.{Task, TaskAggregate}
import task.commands.DeleteTaskCommand
import task.queries.GetTaskByProjectIdAndStartQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeleteTaskValidator(taskAggregate: TaskAggregate, projectAggregate: ProjectAggregate) {
  def validate(command: DeleteTaskCommand): Future[Either[Error, DeleteTaskCommand]] = {
    val taskToDelete = taskAggregate.getTask(GetTaskByProjectIdAndStartQuery(command.projectId, command.start))

    isProjectEmpty
      .andThen(notValidAuthorId)
      .andThen(isProperStartDate)
      .andThen(isProvidedTaskExist)
      .andThen(isProjectAlreadyDeleted)
      .andThen(userIsNotAuthor)
      .apply(DeleteTaskContext(command, taskToDelete))
  }
  val isProjectEmpty: DeleteTaskContext => Either[Error, DeleteTaskContext] =
    (context: DeleteTaskContext) => if (context.command.projectId.isBlank) Left(EmptyProjectId) else Right(context)


  val notValidAuthorId: Either[Error, DeleteTaskContext] => Either[Error, DeleteTaskContext] = {
    case Left(error) => Left(error)
    case Right(context) => if (UUID_NIL.equals(context.command.authorId)) Left(EmptyAuthorId) else Right(context)
  }

  val isProperStartDate: Either[Error, DeleteTaskContext] => Either[Error, DeleteTaskContext] = {
    case Left(error) => Left(error)
    case Right(context) => if(NIL_LOCAL_DATE_TIME.equals(context.command.start))
      Left(IncorrectDate) else Right(context)
  }

  val isProvidedTaskExist: Either[Error, DeleteTaskContext] => Future[Either[Error, DeleteTaskContext]] = {
    case Left(error) => Future.successful(Left(error))
    case Right(context) => isProvidedTaskExist(context)
  }

  private def isProvidedTaskExist(context: DeleteTaskContext): Future[Either[Error, DeleteTaskContext]] = {
    context.taskToDelete
      .map {
        case None => Left(TaskToDeleteNotExist)
        case Some(_) => Right(context)
      }
  }

  val isProjectAlreadyDeleted: Future[Either[Error, DeleteTaskContext]] => Future[Either[Error, DeleteTaskContext]] =
    (result: Future[Either[Error, DeleteTaskContext]]) => {
      result.flatMap{
        case Left(error) => Future.successful(Left(error))
        case Right(context) => isProjectAlreadyDeleted(context)
      }
    }

  private def isProjectAlreadyDeleted(context: DeleteTaskContext): Future[Either[Error, DeleteTaskContext]] ={
    context.taskToDelete.map{
      case Some(task) => if (task.taskTimeDetails.delete.isEmpty) Right(context) else Left(TaskToDeleteAlreadyDeleted)
    }
  }

  val userIsNotAuthor: Future[Either[Error, DeleteTaskContext]] => Future[Either[Error, DeleteTaskCommand]] =
    (result: Future[Either[Error, DeleteTaskContext]]) => {
      result.flatMap{
        case Left(error) => Future.successful(Left(error))
        case Right(context) => isUserAuthorOfProject(context)
      }
    }

  private def isUserAuthorOfProject(context: DeleteTaskContext): Future[Either[Error, DeleteTaskCommand]] = {
    val query = GetProjectByIdAndAuthorIdQuery(context.command.projectId, context.command.authorId)
    projectAggregate.getProject(query)
      .map {
        case None => Left(UserIsNotAuthor)
        case Some(_) => Right(context.command)
      }
  }
}
object DeleteTaskValidator {
  def apply(taskAggregate: TaskAggregate, projectAggregate: ProjectAggregate): DeleteTaskValidator =
    new DeleteTaskValidator(taskAggregate, projectAggregate)
}

case class DeleteTaskContext(command: DeleteTaskCommand, taskToDelete: Future[Option[Task]])
package task.validators

import authentication.{Error, IncorrectDate, TaskToDeleteAlreadyDeleted, TaskToDeleteNotExist}
import common.CommonValidators
import common.LocalDateTimeUtil.NIL_LOCAL_DATE_TIME
import project.ProjectAggregate
import task.TaskAggregate
import task.commands.DeleteTaskCommand
import task.queries.GetTaskByProjectIdAndStartQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeleteTaskValidator(taskAggregate: TaskAggregate, projectAggregate: ProjectAggregate, commonValidators: CommonValidators[DeleteTaskCommand]) {
  def validate(command: DeleteTaskCommand): Future[Either[Error, DeleteTaskCommand]] = {

    commonValidators.isProjectEmpty
      .andThen(commonValidators.notValidAuthorId)
      .andThen(commonValidators.isStartDateCorrect)
      .andThen(isProvidedTaskExist)
      .andThen(isTaskAlreadyDeleted)
      .andThen(commonValidators.userIsNotAuthor)
      .apply(command)
  }

  val isProperStartDate: Either[Error, DeleteTaskCommand] => Either[Error, DeleteTaskCommand] = {
    case Left(error) => Left(error)
    case Right(command) => if(NIL_LOCAL_DATE_TIME.equals(command.start))
      Left(IncorrectDate) else Right(command)
  }

  val isProvidedTaskExist: Either[Error, DeleteTaskCommand] => Future[Either[Error, DeleteTaskCommand]] = {
    case Left(error) => Future.successful(Left(error))
    case Right(context) => isProvidedTaskExist(context)
  }

  private def isProvidedTaskExist(command: DeleteTaskCommand): Future[Either[Error, DeleteTaskCommand]] = {
    taskAggregate.getTask(GetTaskByProjectIdAndStartQuery(command.projectId, command.start))
      .map {
        case None => Left(TaskToDeleteNotExist)
        case Some(_) => Right(command)
      }
  }

  val isTaskAlreadyDeleted: Future[Either[Error, DeleteTaskCommand]] => Future[Either[Error, DeleteTaskCommand]] =
    (result: Future[Either[Error, DeleteTaskCommand]]) => {
      result.flatMap{
        case Left(error) => Future.successful(Left(error))
        case Right(command) => isTaskAlreadyDeleted(command)
      }
    }

  private def isTaskAlreadyDeleted(command: DeleteTaskCommand): Future[Either[Error, DeleteTaskCommand]] =
    taskAggregate.getTask(GetTaskByProjectIdAndStartQuery(command.projectId, command.start)).map {
      case Some(task) => if (task.taskTimeDetails.delete.isEmpty) Right(command) else Left(TaskToDeleteAlreadyDeleted)
  }
}
object DeleteTaskValidator {
  def apply(taskAggregate: TaskAggregate, projectAggregate: ProjectAggregate): DeleteTaskValidator =
    new DeleteTaskValidator(taskAggregate, projectAggregate, new CommonValidators[DeleteTaskCommand](projectAggregate))
}
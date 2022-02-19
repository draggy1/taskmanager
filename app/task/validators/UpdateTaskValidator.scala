package task.validators

import authentication.{Error, IncorrectDate, TaskInConflictWithAnother}
import common.CommonValidators
import common.LocalDateTimeUtil.NIL_LOCAL_DATE_TIME
import project.ProjectAggregate
import task.TaskAggregate
import task.commands.UpdateTaskCommand
import task.queries.GetTaskByProjectIdAndTimeDetailsQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UpdateTaskValidator (taskAggregate: TaskAggregate, commonValidators: CommonValidators[UpdateTaskCommand]) {
  def validate(command: UpdateTaskCommand): Future[Either[Error, UpdateTaskCommand]] =
    commonValidators.isProjectEmpty
      .andThen(commonValidators.notValidAuthorId)
      .andThen(commonValidators.isStartDateCorrect)
      .andThen(commonValidators.isProperDuration)
      .andThen(isNotInConflict)
      .andThen(commonValidators.isProjectExist)
      .andThen(commonValidators.userIsNotAuthor)
      .apply(command)

  val areStartDatesCorrect: Either[Error, UpdateTaskCommand] => Either[Error, UpdateTaskCommand] = {
    case Left(error) => Left(error)
    case Right(command: UpdateTaskCommand) => if(areStartDatesNotCorrect(command)) Left(IncorrectDate) else Right(command)
  }

  private def areStartDatesNotCorrect(command: UpdateTaskCommand) =
    NIL_LOCAL_DATE_TIME.equals(command.taskTimeDetails.start) || NIL_LOCAL_DATE_TIME.equals(command.startDateOld)

  val isNotInConflict: Either[Error, UpdateTaskCommand] => Future[Either[Error, UpdateTaskCommand]] = {
    case Left(error) => Future.successful(Left(error))
    case Right(command: UpdateTaskCommand) => checkIfTaskIsInConflict(command)
  }

  private def checkIfTaskIsInConflict(command: UpdateTaskCommand): Future[Either[Error, UpdateTaskCommand]] = {
    val query = GetTaskByProjectIdAndTimeDetailsQuery(command.projectIdNew, command.taskTimeDetails)
    taskAggregate.getTask(query).map {
      case Some(_) => Left(TaskInConflictWithAnother)
      case None => Right(command)
    }
  }
}

object UpdateTaskValidator {
  def apply(taskAggregate: TaskAggregate, projectAggregate: ProjectAggregate): UpdateTaskValidator =
    new UpdateTaskValidator(taskAggregate, new CommonValidators[UpdateTaskCommand](projectAggregate))
}

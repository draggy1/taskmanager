package task.validators

import authentication.{EmptyProjectId, Error, TaskInConflictWithAnother}
import common.CommonValidators
import project.ProjectAggregate
import task.TaskAggregate
import task.commands.CreateTaskCommand
import task.queries.GetTaskByProjectIdAndTimeDetailsQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CreateTaskValidator(taskAggregate: TaskAggregate, projectAggregate: ProjectAggregate, commonValidators: CommonValidators[CreateTaskCommand]) {
  def validate(command: CreateTaskCommand): Future[Either[Error, CreateTaskCommand]] =
    commonValidators.isProjectEmpty
    .andThen(commonValidators.isStartDateCorrect)
    .andThen(commonValidators.isProperDuration)
    .andThen(isNotInConflict)
    .andThen(commonValidators.isProjectExist)
    .andThen(commonValidators.userIsNotAuthor)
    .apply(command)

  val isProjectEmpty: CreateTaskCommand => Either[Error, CreateTaskCommand] =
    (command: CreateTaskCommand) => if (command.projectId.isBlank) Left(EmptyProjectId) else Right(command)

  val isNotInConflict: Either[Error, CreateTaskCommand] => Future[Either[Error, CreateTaskCommand]] = {
    case Left(error) => Future.successful(Left(error))
    case Right(command: CreateTaskCommand) => checkIfTaskIsInConflict(command)
  }

  private def checkIfTaskIsInConflict(command: CreateTaskCommand): Future[Either[Error, CreateTaskCommand]] = {
    val query = GetTaskByProjectIdAndTimeDetailsQuery(command.projectId, command.taskTimeDetails)
    taskAggregate.getTask(query).map {
      case Some(_) => Left(TaskInConflictWithAnother)
      case None => Right(command)
    }
  }
}

object CreateTaskValidator {
  def apply(taskAggregate: TaskAggregate, projectAggregate: ProjectAggregate): CreateTaskValidator =
    new CreateTaskValidator(taskAggregate, projectAggregate, new CommonValidators[CreateTaskCommand](projectAggregate))
}

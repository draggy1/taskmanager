package task.validators

import authentication.Error
import common.Validator
import project.ProjectAggregate
import task.TaskAggregate
import task.commands.CreateTaskCommand

import scala.concurrent.Future

class CreateTaskValidator(taskAggregate: TaskAggregate, projectAggregate: ProjectAggregate)
  extends Validator[CreateTaskCommand](taskAggregate: TaskAggregate, projectAggregate: ProjectAggregate){

  override def validate(command: CreateTaskCommand): Future[Either[Error, CreateTaskCommand]] =
    isProjectEmpty
    .andThen(isProperStartDate)
    .andThen(isProperDuration)
    .andThen(isNotInConflict)
    .andThen(projectExist)
    .apply(command)
}

object CreateTaskValidator {
  def apply(taskAggregate: TaskAggregate, projectAggregate: ProjectAggregate): CreateTaskValidator =
    new CreateTaskValidator(taskAggregate, projectAggregate)
}

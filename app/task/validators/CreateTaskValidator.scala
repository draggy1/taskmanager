package task.validators

import authentication.Error
import common.CommonValidators
import project.ProjectAggregate
import task.TaskAggregate
import task.commands.CreateTaskCommand

import scala.concurrent.Future

class CreateTaskValidator(commonValidators: CommonValidators[CreateTaskCommand]) {
  def validate(command: CreateTaskCommand): Future[Either[Error, CreateTaskCommand]] =
    commonValidators.isProjectEmpty
    .andThen(commonValidators.isStartDateCorrect)
    .andThen(commonValidators.isProperDuration)
    .andThen(commonValidators.isNotInConflict)
    .andThen(commonValidators.isProjectExist)
    .andThen(commonValidators.userIsNotAuthor)
    .apply(command)
}

object CreateTaskValidator {
  def apply(taskAggregate: TaskAggregate, projectAggregate: ProjectAggregate): CreateTaskValidator =
    new CreateTaskValidator(new CommonValidators[CreateTaskCommand](projectAggregate, taskAggregate))
}

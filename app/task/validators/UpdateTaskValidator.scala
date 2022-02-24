package task.validators

import authentication.Error
import common.CommonValidators
import project.ProjectAggregate
import task.TaskAggregate
import task.commands.UpdateTaskCommand

import scala.concurrent.Future

class UpdateTaskValidator (commonValidators: CommonValidators[UpdateTaskCommand]) {
  def validate(command: UpdateTaskCommand): Future[Either[Error, UpdateTaskCommand]] =
    commonValidators.isProjectEmpty
      .andThen(commonValidators.notValidAuthorId)
      .andThen(commonValidators.isStartDateCorrect)
      .andThen(commonValidators.isProperDuration)
      .andThen(commonValidators.isNotInConflict)
      .andThen(commonValidators.isProjectExist)
      .andThen(commonValidators.userIsNotAuthor)
      .apply(command)
}

object UpdateTaskValidator {
  def apply(taskAggregate: TaskAggregate, projectAggregate: ProjectAggregate): UpdateTaskValidator =
    new UpdateTaskValidator(new CommonValidators[UpdateTaskCommand](projectAggregate, taskAggregate))
}
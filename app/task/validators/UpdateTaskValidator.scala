package task.validators

import authentication.Error
import common.ValidatorFacade
import project.ProjectAggregate
import task.TaskAggregate
import task.commands.UpdateTaskCommand

import scala.concurrent.Future

class UpdateTaskValidator (commonValidators: ValidatorFacade[UpdateTaskCommand]) {
  def validate(command: UpdateTaskCommand): Future[Either[Error, UpdateTaskCommand]] =
    commonValidators.isProjectIdBlank
      .andThen(commonValidators.isAuthorIdBlank)
      .andThen(commonValidators.isStartDateCorrect)
      .andThen(commonValidators.isDurationEmpty)
      .andThen(commonValidators.isNotInConflict)
      .andThen(commonValidators.isProjectExist)
      .andThen(commonValidators.userIsNotAuthor)
      .apply(command)
}

object UpdateTaskValidator {
  def apply(taskAggregate: TaskAggregate, projectAggregate: ProjectAggregate): UpdateTaskValidator =
    new UpdateTaskValidator(new ValidatorFacade[UpdateTaskCommand](projectAggregate, taskAggregate))
}
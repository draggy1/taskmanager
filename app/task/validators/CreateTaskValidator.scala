package task.validators

import authentication.Error
import common.validators.ValidatorFacade
import project.ProjectAggregate
import task.TaskAggregate
import task.commands.CreateTaskCommand

import scala.concurrent.Future

class CreateTaskValidator(commonValidators: ValidatorFacade[CreateTaskCommand]) {
  def validate(command: CreateTaskCommand): Future[Either[Error, CreateTaskCommand]] =
    commonValidators.isProjectIdBlank
    .andThen(commonValidators.isStartDateCorrect)
    .andThen(commonValidators.isDurationEmpty)
    .andThen(commonValidators.isNotInConflict)
    .andThen(commonValidators.isProjectExist)
    .andThen(commonValidators.userIsNotAuthor)
    .apply(command)
}

object CreateTaskValidator {
  def apply(taskAggregate: TaskAggregate, projectAggregate: ProjectAggregate): CreateTaskValidator =
    new CreateTaskValidator(new ValidatorFacade[CreateTaskCommand](projectAggregate, taskAggregate))
}

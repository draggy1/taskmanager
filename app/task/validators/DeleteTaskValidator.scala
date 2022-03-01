package task.validators

import authentication.Error
import common.validators.ValidatorFacade
import project.ProjectAggregate
import task.TaskAggregate
import task.commands.DeleteTaskCommand

import scala.concurrent.Future

class DeleteTaskValidator(commonValidators: ValidatorFacade[DeleteTaskCommand]) {
  def validate(command: DeleteTaskCommand): Future[Either[Error, DeleteTaskCommand]] =
    commonValidators.isProjectIdBlank
    .andThen(commonValidators.isAuthorIdBlank)
    .andThen(commonValidators.isStartDateCorrect)
    .andThen(commonValidators.isProvidedTaskExist)
    .andThen(commonValidators.isTaskAlreadyDeleted)
    .andThen(commonValidators.userIsNotAuthor)
    .apply(command)
}
object DeleteTaskValidator {
  def apply(taskAggregate: TaskAggregate, projectAggregate: ProjectAggregate): DeleteTaskValidator =
    new DeleteTaskValidator(new ValidatorFacade[DeleteTaskCommand](projectAggregate, taskAggregate))
}
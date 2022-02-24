package task.validators

import authentication.Error
import common.CommonValidators
import project.ProjectAggregate
import task.TaskAggregate
import task.commands.DeleteTaskCommand

import scala.concurrent.Future

class DeleteTaskValidator(commonValidators: CommonValidators[DeleteTaskCommand]) {
  def validate(command: DeleteTaskCommand): Future[Either[Error, DeleteTaskCommand]] =
    commonValidators.isProjectEmpty
    .andThen(commonValidators.notValidAuthorId)
    .andThen(commonValidators.isStartDateCorrect)
    .andThen(commonValidators.isProvidedTaskExist)
    .andThen(commonValidators.isTaskAlreadyDeleted)
    .andThen(commonValidators.userIsNotAuthor)
    .apply(command)
}
object DeleteTaskValidator {
  def apply(taskAggregate: TaskAggregate, projectAggregate: ProjectAggregate): DeleteTaskValidator =
    new DeleteTaskValidator(new CommonValidators[DeleteTaskCommand](projectAggregate, taskAggregate))
}
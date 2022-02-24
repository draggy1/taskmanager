package project.validators

import authentication.Error
import common.CommonValidators
import project.ProjectAggregate
import project.commands.DeleteProjectCommand
import task.TaskAggregate

import scala.concurrent.Future

case class DeleteProjectValidator(commonValidator: CommonValidators[DeleteProjectCommand]) {
  def validate(command: DeleteProjectCommand): Future[Either[Error, DeleteProjectCommand]] =
    commonValidator.isProjectEmpty
    .andThen(commonValidator.notValidAuthorId)
    .andThen(commonValidator.mapToFuture)
    .andThen(commonValidator.isProjectExist)
    .andThen(commonValidator.userIsNotAuthor)
    .andThen(commonValidator.isProjectAlreadyDeleted)
    .apply(command)
}

object DeleteProjectValidator {
  def apply(projectAggregate: ProjectAggregate, taskAggregate: TaskAggregate): DeleteProjectValidator =
    new DeleteProjectValidator(new CommonValidators[DeleteProjectCommand](projectAggregate, taskAggregate))
}
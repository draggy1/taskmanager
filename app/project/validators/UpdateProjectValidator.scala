package project.validators

import authentication.Error
import common.CommonValidators
import project.ProjectAggregate
import project.commands.UpdateProjectCommand
import task.TaskAggregate

import scala.concurrent.Future

class UpdateProjectValidator(commonValidator: CommonValidators[UpdateProjectCommand]) {
  def validate(command: UpdateProjectCommand): Future[Either[Error, UpdateProjectCommand]] =
    commonValidator.isProjectEmpty
      .andThen(commonValidator.notValidAuthorId)
      .andThen(commonValidator.isDuplicated)
      .andThen(commonValidator.isProjectExist)
      .andThen(commonValidator.userIsNotAuthor)
      .apply(command)
}

case object UpdateProjectValidator {
  def apply(projectAggregate: ProjectAggregate, taskAggregate: TaskAggregate): UpdateProjectValidator =
    new UpdateProjectValidator(new CommonValidators[UpdateProjectCommand](projectAggregate, taskAggregate))
}

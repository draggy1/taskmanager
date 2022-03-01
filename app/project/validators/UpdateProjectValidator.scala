package project.validators

import authentication.Error
import common.validators.ValidatorFacade
import project.ProjectAggregate
import project.commands.UpdateProjectCommand
import task.TaskAggregate

import scala.concurrent.Future

class UpdateProjectValidator(commonValidator: ValidatorFacade[UpdateProjectCommand]) {
  def validate(command: UpdateProjectCommand): Future[Either[Error, UpdateProjectCommand]] =
    commonValidator.isProjectIdBlank
      .andThen(commonValidator.isAuthorIdBlank)
      .andThen(commonValidator.isProjectDuplicated)
      .andThen(commonValidator.isProjectExist)
      .andThen(commonValidator.userIsNotAuthor)
      .apply(command)
}

case object UpdateProjectValidator {
  def apply(projectAggregate: ProjectAggregate, taskAggregate: TaskAggregate): UpdateProjectValidator =
    new UpdateProjectValidator(new ValidatorFacade[UpdateProjectCommand](projectAggregate, taskAggregate))
}

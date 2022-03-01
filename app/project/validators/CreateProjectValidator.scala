package project.validators

import authentication.Error
import common.validators.ValidatorFacade
import project.ProjectAggregate
import project.commands.CreateProjectCommand
import task.TaskAggregate

import scala.concurrent.Future

class CreateProjectValidator(commonValidator: ValidatorFacade[CreateProjectCommand]) {

  def validate(command: CreateProjectCommand): Future[Either[Error, CreateProjectCommand]] = {
    commonValidator.isProjectIdBlank
      .andThen(commonValidator.isAuthorIdBlank)
      .andThen(commonValidator.isProjectDuplicated)
      .apply(command)
  }
}

object CreateProjectValidator {
  def apply(projectAggregate: ProjectAggregate, taskAggregate: TaskAggregate): CreateProjectValidator =
    new CreateProjectValidator(new ValidatorFacade[CreateProjectCommand](projectAggregate, taskAggregate))
}

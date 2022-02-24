package project.validators

import authentication.Error
import common.CommonValidators
import project.ProjectAggregate
import project.commands.CreateProjectCommand
import task.TaskAggregate

import scala.concurrent.Future

class CreateProjectValidator(commonValidator: CommonValidators[CreateProjectCommand]) {

  def validate(command: CreateProjectCommand): Future[Either[Error, CreateProjectCommand]] = {
    commonValidator.isProjectEmpty
      .andThen(commonValidator.notValidAuthorId)
      .andThen(commonValidator.isDuplicated)
      .apply(command)
  }
}

object CreateProjectValidator {
  def apply(projectAggregate: ProjectAggregate, taskAggregate: TaskAggregate): CreateProjectValidator =
    new CreateProjectValidator(new CommonValidators[CreateProjectCommand](projectAggregate, taskAggregate))
}

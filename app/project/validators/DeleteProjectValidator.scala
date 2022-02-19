package project.validators

import authentication.{Error, ProjectToDeleteAlreadyDeleted}
import common.CommonValidators
import project.ProjectAggregate
import project.commands.DeleteProjectCommand
import project.queries.GetProjectByIdQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class DeleteProjectValidator(projectAggregate: ProjectAggregate, commonValidator: CommonValidators[DeleteProjectCommand]) {
  def validate(command: DeleteProjectCommand): Future[Either[Error, DeleteProjectCommand]] =
    commonValidator.isProjectEmpty
    .andThen(commonValidator.notValidAuthorId)
    .andThen(commonValidator.mapToFuture)
    .andThen(commonValidator.isProjectExist)
    .andThen(commonValidator.userIsNotAuthor)
    .andThen(isProjectAlreadyDeleted)
    .apply(command)

  val isProjectAlreadyDeleted: Future[Either[Error, DeleteProjectCommand]] => Future[Either[Error, DeleteProjectCommand]] =
    (result: Future[Either[Error, DeleteProjectCommand]]) => result.flatMap {
      case Left(error) => Future.successful(Left(error))
      case Right(command) => isProjectAlreadyDeleted(command)
    }

  private def isProjectAlreadyDeleted(command: DeleteProjectCommand): Future[Either[Error, DeleteProjectCommand]] =
    projectAggregate.getProject(GetProjectByIdQuery(command.projectId)).map {
    case Some(task) => if(task.deleted.isEmpty) Right(command) else Left(ProjectToDeleteAlreadyDeleted)
  }
}

object DeleteProjectValidator {
  def apply(projectAggregate: ProjectAggregate): DeleteProjectValidator =
    new DeleteProjectValidator(projectAggregate, new CommonValidators[DeleteProjectCommand](projectAggregate))
}
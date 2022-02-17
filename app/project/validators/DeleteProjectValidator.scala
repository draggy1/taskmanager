package project.validators

import authentication.{EmptyAuthorId, EmptyProjectId, Error, ProjectIdNotFound, ProjectToDeleteAlreadyDeleted, UserIsNotAuthor}
import common.UUIDUtils.UUID_NIL
import common.ValidationContext
import project.ProjectAggregate
import project.commands.DeleteProjectCommand
import project.queries.GetProjectByIdQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class DeleteProjectValidator(projectAggregate: ProjectAggregate) {
  def validate(command: DeleteProjectCommand): Future[Either[Error, DeleteProjectCommand]] = {
    val project = getProject(command.projectId)

    isProjectEmpty
      .andThen(notValidAuthorId)
      .andThen(isProjectExist)
      .andThen(userIsNotAuthor)
      .andThen(isProjectAlreadyDeleted)
      .apply(ValidationContext(command, project, Future.successful(Option.empty)))
      .map {
        case Left(error) => Left(error)
        case Right(context) => Right(context.command)
      }
  }

  val isProjectEmpty: ValidationContext[DeleteProjectCommand] => Either[Error, ValidationContext[DeleteProjectCommand]] =
    (context: ValidationContext[DeleteProjectCommand]) => if (context.command.projectId.isBlank) Left(EmptyProjectId) else Right(context)

  val notValidAuthorId: Either[Error, ValidationContext[DeleteProjectCommand]] => Either[Error, ValidationContext[DeleteProjectCommand]] = {
    case Left(error) => Left(error)
    case Right(context) => if (UUID_NIL.equals(context.command.authorId)) Left(EmptyAuthorId) else Right(context)
  }

  val isProjectExist: Either[Error, ValidationContext[DeleteProjectCommand]] => Future[Either[Error, ValidationContext[DeleteProjectCommand]]] = {
      case Left(error) => Future.successful(Left(error))
      case Right(context) => isProjectExist(context)
  }

  val userIsNotAuthor: Future[Either[Error, ValidationContext[DeleteProjectCommand]]] => Future[Either[Error, ValidationContext[DeleteProjectCommand]]] =
    (result: Future[Either[Error, ValidationContext[DeleteProjectCommand]]]) => result.flatMap {
        case Left(error) => Future.successful(Left(error))
        case Right(context) => isUserAuthorOfProject(context)
    }

  val isProjectAlreadyDeleted: Future[Either[Error, ValidationContext[DeleteProjectCommand]]] => Future[Either[Error, ValidationContext[DeleteProjectCommand]]] =
    (result: Future[Either[Error, ValidationContext[DeleteProjectCommand]]]) => result.flatMap {
      case Left(error) => Future.successful(Left(error))
      case Right(context) => isProjectAlreadyDeleted(context)
    }

  private def isUserAuthorOfProject(context: ValidationContext[DeleteProjectCommand]): Future[Either[Error, ValidationContext[DeleteProjectCommand]]] = {
    context.project
      .map {
        case Some(project) => if(project.authorId.equals(context.command.authorId))
          Right(context) else Left(UserIsNotAuthor)
      }
  }

  private def isProjectExist(context: ValidationContext[DeleteProjectCommand]) = {
    context.project
      .map {
        case None => Left(ProjectIdNotFound)
        case Some(_) => Right(context)
      }
  }

  private def isProjectAlreadyDeleted(context: ValidationContext[DeleteProjectCommand]): Future[Either[Error, ValidationContext[DeleteProjectCommand]]] = {
    val task = context.project
    task.map {
      case Some(task) => if(task.deleted.isEmpty) Right(context) else Left(ProjectToDeleteAlreadyDeleted)
    }
  }

  private def getProject(projectId: String) = projectAggregate.getProject(GetProjectByIdQuery(projectId))
}

object DeleteProjectValidator {
  def apply(projectAggregate: ProjectAggregate): DeleteProjectValidator = new DeleteProjectValidator(projectAggregate)
}
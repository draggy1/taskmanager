package project.validators

import authentication.{EmptyAuthorId, EmptyProjectId, Error, ProjectIdNotFound, ProjectToDeleteAlreadyDeleted, UserIsNotAuthor}
import common.UUIDUtils.UUID_NIL
import project.commands.DeleteProjectCommand
import project.queries.GetProjectByIdQuery
import project.{Project, ProjectAggregate}

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
      .apply(DeleteProjectContext(command, project))
  }

  val isProjectEmpty: DeleteProjectContext => Either[Error, DeleteProjectContext] =
    (context: DeleteProjectContext) => if (context.command.projectId.isBlank) Left(EmptyProjectId) else Right(context)

  val notValidAuthorId: Either[Error, DeleteProjectContext] => Either[Error, DeleteProjectContext] = {
    case Left(error) => Left(error)
    case Right(context) => if (UUID_NIL.equals(context.command.authorId)) Left(EmptyAuthorId) else Right(context)
  }

  val isProjectExist: Either[Error, DeleteProjectContext] => Future[Either[Error, DeleteProjectContext]] = {
      case Left(error) => Future.successful(Left(error))
      case Right(context) => isProjectExist(context)
  }

  val userIsNotAuthor: Future[Either[Error, DeleteProjectContext]] => Future[Either[Error, DeleteProjectContext]] =
    (result: Future[Either[Error, DeleteProjectContext]]) => result.flatMap {
        case Left(error) => Future.successful(Left(error))
        case Right(context) => isUserAuthorOfProject(context)
    }

  val isProjectAlreadyDeleted: Future[Either[Error, DeleteProjectContext]] => Future[Either[Error, DeleteProjectCommand]] =
    (result: Future[Either[Error, DeleteProjectContext]]) => result.flatMap {
      case Left(error) => Future.successful(Left(error))
      case Right(context) => isProjectAlreadyDeleted(context)
    }

  private def isUserAuthorOfProject(context: DeleteProjectContext): Future[Either[Error, DeleteProjectContext]] = {
    context.project
      .map {
        case Some(project) => if(project.authorId.equals(context.command.authorId))
          Right(context) else Left(UserIsNotAuthor)
      }
  }

  private def isProjectExist(context: DeleteProjectContext) = {
    context.project
      .map {
        case None => Left(ProjectIdNotFound)
        case Some(_) => Right(context)
      }
  }

  private def isProjectAlreadyDeleted(context: DeleteProjectContext): Future[Either[Error, DeleteProjectCommand]] = {
    val task = context.project
    task.map {
      case Some(task) => if(task.deleted.isEmpty) Right(context.command) else Left(ProjectToDeleteAlreadyDeleted)
    }
  }

  private def getProject(projectId: String) = projectAggregate.getProject(GetProjectByIdQuery(projectId))
}

object DeleteProjectValidator {
  def apply(projectAggregate: ProjectAggregate): DeleteProjectValidator = new DeleteProjectValidator(projectAggregate)
}

case class DeleteProjectContext(command: DeleteProjectCommand, project: Future[Option[Project]])
package common

import authentication.{DuplicatedProjectId, EmptyAuthorId, EmptyProjectId, Error, ProjectIdNotFound, UserIsNotAuthor}
import common.UUIDUtils.UUID_NIL
import project.ProjectAggregate
import project.queries.GetProjectByIdQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CommonValidators[C <: Command](aggregate: ProjectAggregate) {
  val isProjectEmpty: C => Either[Error, C] =
    (command: C) => {
      if (command.checkIfProjectIdIsBlank()) Left(EmptyProjectId) else Right(command)
    }

  val notValidAuthorId: Either[Error, C] => Either[Error, C] = {
    case Left(error) => Left(error)
    case Right(command) => if (UUID_NIL.equals(command.getAuthorId)) Left(EmptyAuthorId) else Right(command)
  }

  val isDuplicated: Either[Error, C] => Future[Either[Error, C]] = {
    case Left(error) => Future.successful(Left(error))
    case Right(command) => isDuplicated(command)
  }

  val mapToFuture: Either[Error, C] => Future[Either[Error, C]] = either => Future.successful(either)

  private def isDuplicated(command: C) =
    aggregate.getProject(GetProjectByIdQuery(command.getProjectId))
      .map {
        case Some(_) => Left(DuplicatedProjectId)
        case None => Right(command)
      }

  val isProjectExist: Future[Either[Error, C]] => Future[Either[Error, C]] =
    (result: Future[Either[Error, C]]) => result.flatMap {
      case Left(error) => Future.successful(Left(error))
      case Right(command) => isProjectExist(command)
    }

  private def isProjectExist(command: C) = {
    aggregate.getProject(GetProjectByIdQuery(command.getProjectToCheckIfExist))
      .map {
        case None => Left(ProjectIdNotFound)
        case Some(_) => Right(command)
      }
  }

  val userIsNotAuthor: Future[Either[Error, C]] => Future[Either[Error, C]] =
    (result: Future[Either[Error, C]]) => result.flatMap {
      case Left(error) => Future.successful(Left(error))
      case Right(command) => isUserAuthorOfProject(command)
    }

  private def isUserAuthorOfProject(command: C): Future[Either[Error, C]] = {
    aggregate.getProject(GetProjectByIdQuery(command.getProjectToCheckIfExist))
      .map {
        case Some(project) => if(project.authorId.equals(command.getAuthorId))
          Right(command) else Left(UserIsNotAuthor)
      }
  }
}

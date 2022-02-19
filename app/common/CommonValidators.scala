package common

import authentication.{DuplicatedProjectId, EmptyAuthorId, EmptyProjectId, Error, IncorrectDate, IncorrectDuration, ProjectIdNotFound, ProjectToDeleteAlreadyDeleted, UserIsNotAuthor}
import project.ProjectAggregate
import project.queries.GetProjectByIdQuery
import task.TaskDuration.TASK_DURATION_EMPTY

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CommonValidators[C <: Command](aggregate: ProjectAggregate) {
  val isProjectEmpty: C => Either[Error, C] =
    (command: C) => {
      if (command.isProjectIdBlank) Left(EmptyProjectId) else Right(command)
    }

  val notValidAuthorId: Either[Error, C] => Either[Error, C] = {
    case Left(error) => Left(error)
    case Right(command) => if (command.isAuthorIdBlank) Left(EmptyAuthorId) else Right(command)
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

  val isProjectAlreadyDeleted: Future[Either[Error, C]] => Future[Either[Error, C]] =
    (result: Future[Either[Error, C]]) => result.flatMap {
      case Left(error) => Future.successful(Left(error))
      case Right(command) => isProjectAlreadyDeleted(command)
    }

  private def isProjectAlreadyDeleted(command: C): Future[Either[Error, C]] =
    aggregate.getProject(GetProjectByIdQuery(command.getProjectId)).map {
      case Some(project) => if(project.deleted.isEmpty) Right(command) else Left(ProjectToDeleteAlreadyDeleted)
    }

  val isProperDuration: Either[Error, C with WithTaskTimeDetails] => Either[Error, C] = {
    case Left(error) => Left(error)
    case Right(command) => if(TASK_DURATION_EMPTY.equals(command.getTimeDetails.duration))
      Left(IncorrectDuration) else Right(command)
  }

  val isStartDateCorrect: Either[Error, C with WithStart] => Either[Error, C] = {
    case Left(error) => Left(error)
    case Right(command: C) => if(command.isStartDateNotCorrect)
      Left(IncorrectDate) else Right(command)
  }
}

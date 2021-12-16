package project.validators

import authentication.{DuplicatedProjectId, EmptyAuthorId, EmptyProjectId, Error, ProjectIdNotFound, UserIsNotAuthor}
import common.UUIDUtils.UUID_NIL
import project.ProjectAggregate
import project.commands.UpdateProjectCommand
import project.queries.{GetProjectByIdAndAuthorIdQuery, GetProjectByIdQuery}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UpdateProjectValidator(aggregate: ProjectAggregate) {
  def validate(command: UpdateProjectCommand): Future[Either[Error, UpdateProjectCommand]] =
  isProjectEmpty
      .andThen(notValidAuthorId)
      .andThen(isDuplicated)
      .andThen(isProjectExist)
      .andThen(userIsNotAuthor)
      .apply(command)

  val isProjectEmpty: UpdateProjectCommand => Either[Error, UpdateProjectCommand] =
    (command: UpdateProjectCommand) => {
      if (isAnyProjectIdBlank(command)) Left(EmptyProjectId) else Right(command)
    }

  private def isAnyProjectIdBlank(command: UpdateProjectCommand) =
    command.projectIdOld.isBlank || command.projectIdNew.isBlank


  val notValidAuthorId: Either[Error, UpdateProjectCommand] => Either[Error, UpdateProjectCommand] = {
    case Left(error) => Left(error)
    case Right(command) => if (UUID_NIL.equals(command.authorId)) Left(EmptyAuthorId) else Right(command)
  }

  val isDuplicated: Either[Error, UpdateProjectCommand] => Future[Either[Error, UpdateProjectCommand]] = {
    case Left(error) => Future.successful(Left(error))
    case Right(command) => isDuplicated(command)
  }

  private def isDuplicated(command: UpdateProjectCommand) =
    getProject(command.projectIdNew)
      .map {
        case Some(_) => Left(DuplicatedProjectId)
        case None => Right(command)
      }

  val isProjectExist: Future[Either[Error, UpdateProjectCommand]] => Future[Either[Error, UpdateProjectCommand]] =
    (result: Future[Either[Error, UpdateProjectCommand]]) => result.flatMap {
      case Left(error) => Future.successful(Left(error))
      case Right(command) => isProjectExist(command)
  }

  val userIsNotAuthor: Future[Either[Error, UpdateProjectCommand]] => Future[Either[Error, UpdateProjectCommand]] =
    (result: Future[Either[Error, UpdateProjectCommand]]) => {
      result.flatMap{
        case Left(error) => Future.successful(Left(error))
        case Right(command) => isUserAuthorOfProject(command)
      }
    }

  private def isUserAuthorOfProject(command: UpdateProjectCommand): Future[Either[Error, UpdateProjectCommand]] = {
    aggregate.getProject(GetProjectByIdAndAuthorIdQuery(command.projectIdOld, command.authorId))
      .map {
        case None => Left(UserIsNotAuthor)
        case Some(_) => Right(command)
      }
  }

  private def isProjectExist(command: UpdateProjectCommand) = {
    getProject(command.projectIdOld)
      .map {
        case None => Left(ProjectIdNotFound)
        case Some(_) => Right(command)
      }
  }

  private def getProject(projectId: String) = aggregate.getProject(GetProjectByIdQuery(projectId))
}

case object UpdateProjectValidator {
  def apply(aggregate: ProjectAggregate): UpdateProjectValidator = new UpdateProjectValidator(aggregate)
}

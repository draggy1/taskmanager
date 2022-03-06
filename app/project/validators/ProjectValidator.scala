package project.validators

import authentication.{DuplicatedProjectId, Error, ProjectIdNotFound, ProjectToDeleteAlreadyDeleted, UserIsNotAuthor}
import common.Command
import project.ProjectAggregate
import project.queries.GetProjectByIdQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class ProjectValidator[C <: Command](aggregate: ProjectAggregate) {
  def isProjectDuplicated(command: C): Future[Either[DuplicatedProjectId.type, C]] = {
    aggregate.getProject(GetProjectByIdQuery(command.getProjectId))
      .map {
        case Some(_) => Left(DuplicatedProjectId)
        case None => Right(command)
      }
  }

  def isProjectExist(command: C): Future[Either[ProjectIdNotFound.type, C]] =
    aggregate.getProject(GetProjectByIdQuery(command.getProjectToCheckIfExist))
      .map {
        case None => Left(ProjectIdNotFound)
        case Some(_) => Right(command)
      }

  def isUserAuthorOfProject(command: C): Future[Either[Error, C]] =
    aggregate.getProject(GetProjectByIdQuery(command.getProjectToCheckIfExist))
      .map {
        case Some(project) => if(project.authorId.equals(command.getAuthorId))
          Right(command) else Left(UserIsNotAuthor)
      }

  def isProjectAlreadyDeleted(command: C): Future[Either[Error, C]] =
    aggregate.getProject(GetProjectByIdQuery(command.getProjectId))
      .map {
        case Some(project) => if(project.deleted.isEmpty) Right(command) else Left(ProjectToDeleteAlreadyDeleted)
    }
}

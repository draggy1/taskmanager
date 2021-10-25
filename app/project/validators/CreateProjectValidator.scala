package project.validators

import authentication.{DuplicatedProjectId, EmptyProjectId, EmptyUserId, Error}
import common.UUIDUtils.UUID_NIL
import project.ProjectAggregate
import project.commands.CreateProjectCommand
import project.queries.GetProjectByIdQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CreateProjectValidator(aggregate: ProjectAggregate) {
  def validate(command: CreateProjectCommand): Future[Either[Error, CreateProjectCommand]] =
    isProjectEmpty
      .andThen(notValidUserId)
      .andThen(isDuplicated)
      .apply(command)

  val isProjectEmpty: CreateProjectCommand => Either[Error, CreateProjectCommand] =
    (command: CreateProjectCommand) => {
      if (command.projectId.isBlank) Left(EmptyProjectId) else Right(command)
    }

  val notValidUserId: Either[Error, CreateProjectCommand] => Either[Error, CreateProjectCommand] = {
    case Left(error) => Left(error)
    case Right(command) => if (UUID_NIL.equals(command.userId)) Left(EmptyUserId) else Right(command)
  }

  val isDuplicated: Either[Error, CreateProjectCommand] => Future[Either[Error, CreateProjectCommand]] = {
    case Left(error) => Future.successful(Left(error))
    case Right(command) => getProject(command)
  }

  private def getProject(command: CreateProjectCommand) = {
    val eventualMaybeProject = aggregate.getProject(GetProjectByIdQuery(command.projectId))
    eventualMaybeProject
      .map {
        case Some(_) => Left(DuplicatedProjectId)
        case None => Right(command)
      }
  }
}

object CreateProjectValidator {
  def apply(aggregate: ProjectAggregate): CreateProjectValidator = new CreateProjectValidator(aggregate)
}

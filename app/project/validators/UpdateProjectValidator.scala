package project.validators

import authentication.{DuplicatedProjectId, EmptyProjectId, EmptyUserId, Error}
import common.UUIDUtils.UUID_NIL
import project.ProjectAggregate
import project.commands.UpdateProjectCommand
import project.queries.GetProjectByIdQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UpdateProjectValidator(aggregate: ProjectAggregate) {
  def validate(command: UpdateProjectCommand): Future[Either[Error, UpdateProjectCommand]] =
  isProjectEmpty
      .andThen(notValidUserId)
      .andThen(isDuplicated)
      .apply(command)

  val isProjectEmpty: UpdateProjectCommand => Either[Error, UpdateProjectCommand] =
    (command: UpdateProjectCommand) => {
      if (command.projectIdOld.isBlank) Left(EmptyProjectId) else Right(command)
    }

  val notValidUserId: Either[Error, UpdateProjectCommand] => Either[Error, UpdateProjectCommand] = {
    case Left(error) => Left(error)
    case Right(command) => if (UUID_NIL.equals(command.authorId)) Left(EmptyUserId) else Right(command)
  }

  val isDuplicated: Either[Error, UpdateProjectCommand] => Future[Either[Error, UpdateProjectCommand]] = {
    case Left(error) => Future.successful(Left(error))
    case Right(command) => getProject(command)
  }

  private def getProject(command: UpdateProjectCommand) = {
    val eventualMaybeProject = aggregate.getProject(GetProjectByIdQuery(command.projectIdNew))
    eventualMaybeProject
      .map {
        case Some(_) => Left(DuplicatedProjectId)
        case None => Right(command)
      }
  }
}
case object UpdateProjectValidator {
  def apply(aggregate: ProjectAggregate): UpdateProjectValidator = new UpdateProjectValidator(aggregate)
}

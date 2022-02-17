package project.validators

import authentication.{DuplicatedProjectId, EmptyAuthorId, EmptyProjectId, Error}
import common.UUIDUtils.UUID_NIL
import common.ValidationContext
import project.ProjectAggregate
import project.commands.CreateProjectCommand
import project.queries.GetProjectByIdQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CreateProjectValidator(aggregate: ProjectAggregate) {
  def validate(command: CreateProjectCommand): Future[Either[Error, CreateProjectCommand]] = {
    val project = aggregate.getProject(GetProjectByIdQuery(command.projectId))
    isProjectEmpty
      .andThen(notValidAuthorId)
      .andThen(isDuplicated)
      .apply(ValidationContext(command, project, Future.successful(Option.empty)))
      .map {
        case Left(error) => Left(error)
        case Right(context) => Right(context.command)
      }
  }

  val isProjectEmpty: ValidationContext[CreateProjectCommand] => Either[Error, ValidationContext[CreateProjectCommand]] =
    (context: ValidationContext[CreateProjectCommand]) => {
      if (context.command.projectId.isBlank) Left(EmptyProjectId) else Right(context)
    }

  val notValidAuthorId: Either[Error, ValidationContext[CreateProjectCommand]] => Either[Error, ValidationContext[CreateProjectCommand]] = {
    case Left(error) => Left(error)
    case Right(context) => if (UUID_NIL.equals(context.command.authorId)) Left(EmptyAuthorId) else Right(context)
  }

  val isDuplicated: Either[Error, ValidationContext[CreateProjectCommand]] => Future[Either[Error, ValidationContext[CreateProjectCommand]]] = {
    case Left(error) => Future.successful(Left(error))
    case Right(context) => isDuplicated(context)
  }

  private def isDuplicated(context: ValidationContext[CreateProjectCommand]) = {
    context.project
      .map {
        case Some(_) => Left(DuplicatedProjectId)
        case None => Right(context)
      }
  }
}

object CreateProjectValidator {
  def apply(aggregate: ProjectAggregate): CreateProjectValidator = new CreateProjectValidator(aggregate)
}

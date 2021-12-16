package task.validators

import authentication.{EmptyProjectId, Error, IncorrectDate, IncorrectDuration, ProjectIdNotFound, TaskInConflictWithAnother}
import common.LocalDateTimeUtil.NIL_LOCAL_DATE_TIME
import project.ProjectAggregate
import project.queries.GetProjectByIdAndAuthorIdQuery
import task.TaskAggregate
import task.TaskDuration.TASK_DURATION_EMPTY
import task.commands.CreateTaskCommand
import task.queries.GetTaskByProjectIdAndTimeDetailsQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CreateTaskValidator(taskAggregate: TaskAggregate, projectAggregate: ProjectAggregate) {
  def validate(command: CreateTaskCommand): Future[Either[Error, CreateTaskCommand]] = isProjectEmpty
    .andThen(isProperStartDate)
    .andThen(isProperDuration)
    .andThen(isNotInConflict)
    .andThen(projectExist)
    .apply(command)

  val isProjectEmpty: CreateTaskCommand => Either[Error, CreateTaskCommand] =
    (command: CreateTaskCommand) => if (command.projectId.isBlank) Left(EmptyProjectId) else Right(command)

  val isProperStartDate: Either[Error, CreateTaskCommand] => Either[Error, CreateTaskCommand] = {
      case Left(error) => Left(error)
      case Right(command: CreateTaskCommand) => if(NIL_LOCAL_DATE_TIME.equals(command.taskTimeDetails.start))
        Left(IncorrectDate) else Right(command)
  }

  val isProperDuration: Either[Error, CreateTaskCommand] => Either[Error, CreateTaskCommand] = {
    case Left(error) => Left(error)
    case Right(command: CreateTaskCommand) => if(TASK_DURATION_EMPTY.equals(command.taskTimeDetails.duration))
      Left(IncorrectDuration) else Right(command)
  }

  val isNotInConflict: Either[Error, CreateTaskCommand] => Future[Either[Error, CreateTaskCommand]] = {
    case Left(error) => Future.successful(Left(error))
    case Right(command: CreateTaskCommand) => checkIfTaskIsInConflict(command)
  }

  private def checkIfTaskIsInConflict(command: CreateTaskCommand): Future[Either[Error, CreateTaskCommand]] = {
    val query = GetTaskByProjectIdAndTimeDetailsQuery(command.projectId, command.taskTimeDetails)
    taskAggregate.getTask(query).map {
      case Some(_) => Left(TaskInConflictWithAnother)
      case None => Right(command)
    }
  }

  val projectExist: Future[Either[Error, CreateTaskCommand]] => Future[Either[Error, CreateTaskCommand]] =
    (result: Future[Either[Error, CreateTaskCommand]]) => result.flatMap {
      case Left(error) => Future.successful(Left(error))
      case Right(command) => projectExist(command)
    }

  private def projectExist(command: CreateTaskCommand) = {
    val query = GetProjectByIdAndAuthorIdQuery(command.projectId, command.authorId)
    projectAggregate.getProject(query)
      .map {
        case None => Left(ProjectIdNotFound)
        case Some(_) => Right(command)
      }
  }
}

object CreateTaskValidator {
  def apply(taskAggregate: TaskAggregate, projectAggregate: ProjectAggregate): CreateTaskValidator =
    new CreateTaskValidator(taskAggregate, projectAggregate)
}

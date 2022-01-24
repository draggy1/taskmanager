package task.validators

import authentication.{EmptyAuthorId, EmptyProjectId, Error, IncorrectDate, IncorrectDuration, ProjectIdNotFound, TaskInConflictWithAnother}
import common.TimeUtils.NIL_LOCAL_DATE_TIME
import common.UUIDUtils.UUID_NIL
import project.ProjectAggregate
import project.queries.GetProjectByIdAndAuthorIdQuery
import task.TaskAggregate
import task.TaskDuration.TASK_DURATION_EMPTY
import task.commands.UpdateTaskCommand
import task.queries.GetTaskByProjectIdAndTimeDetailsQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UpdateTaskValidator (taskAggregate: TaskAggregate, projectAggregate: ProjectAggregate) {
  def validate(command: UpdateTaskCommand): Future[Either[Error, UpdateTaskCommand]] =
    areProjectIdsEmpty
      .andThen(areAuthorIdsEmpty)
      .andThen(areStartDatesCorrect)
      .andThen(isProperDuration)
      .andThen(isNotInConflict)
      .andThen(projectExist)
      .apply(command)

  val areProjectIdsEmpty: UpdateTaskCommand => Either[Error, UpdateTaskCommand] =
    (command: UpdateTaskCommand) => if(isAnyProjectIdBlank(command)) Left(EmptyProjectId) else Right(command)

  private def isAnyProjectIdBlank(command: UpdateTaskCommand) =
    command.projectIdOld.isBlank || command.projectIdNew.isBlank

  val areAuthorIdsEmpty: Either[Error, UpdateTaskCommand] => Either[Error, UpdateTaskCommand]  = {
    case Left(error) => Left(error)
    case Right(command: UpdateTaskCommand) =>
      if(isAnyAuthorIdBlank(command)) Left(EmptyAuthorId) else Right(command)
  }

  private def isAnyAuthorIdBlank(command: UpdateTaskCommand) =
    UUID_NIL.equals(command.authorIdOld) || UUID_NIL.equals(command.authorIdNew)

  val areStartDatesCorrect: Either[Error, UpdateTaskCommand] => Either[Error, UpdateTaskCommand] = {
    case Left(error) => Left(error)
    case Right(command: UpdateTaskCommand) => if(areStartDatesNotCorrect(command)) Left(IncorrectDate) else Right(command)
  }

  private def areStartDatesNotCorrect(command: UpdateTaskCommand) =
    NIL_LOCAL_DATE_TIME.equals(command.taskTimeDetails.start) || NIL_LOCAL_DATE_TIME.equals(command.startDateOld)


  val isProperDuration: Either[Error, UpdateTaskCommand] => Either[Error, UpdateTaskCommand] = {
    case Left(error) => Left(error)
    case Right(command: UpdateTaskCommand) => if(TASK_DURATION_EMPTY.equals(command.taskTimeDetails.duration))
      Left(IncorrectDuration) else Right(command)
  }

  val isNotInConflict: Either[Error, UpdateTaskCommand] => Future[Either[Error, UpdateTaskCommand]] = {
    case Left(error) => Future.successful(Left(error))
    case Right(command: UpdateTaskCommand) => checkIfTaskIsInConflict(command)
  }

  private def checkIfTaskIsInConflict(command: UpdateTaskCommand): Future[Either[Error, UpdateTaskCommand]] = {
    val query = GetTaskByProjectIdAndTimeDetailsQuery(command.projectIdNew, command.taskTimeDetails)
    taskAggregate.getTask(query).map {
      case Some(_) => Left(TaskInConflictWithAnother)
      case None => Right(command)
    }
  }

  val projectExist: Future[Either[Error, UpdateTaskCommand]] => Future[Either[Error, UpdateTaskCommand]] =
    (result: Future[Either[Error, UpdateTaskCommand]]) => result.flatMap {
      case Left(error) => Future.successful(Left(error))
      case Right(command) => projectExist(command)
    }

  private def projectExist(command: UpdateTaskCommand) = {
    val query = GetProjectByIdAndAuthorIdQuery(command.projectIdNew, command.authorIdNew)
    projectAggregate.getProject(query)
      .map {
        case None => Left(ProjectIdNotFound)
        case Some(_) => Right(command)
      }
  }
}

object UpdateTaskValidator {
  def apply(taskAggregate: TaskAggregate, projectAggregate: ProjectAggregate): UpdateTaskValidator =
    new UpdateTaskValidator(taskAggregate, projectAggregate)
}

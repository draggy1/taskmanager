package common

import project.ProjectAggregate
import task.TaskAggregate
import authentication.{EmptyProjectId, Error, IncorrectDate, IncorrectDuration, ProjectIdNotFound, TaskInConflictWithAnother}
import common.TimeUtils.NIL_LOCAL_DATE_TIME
import project.queries.GetProjectByIdAndAuthorIdQuery
import task.TaskDuration.TASK_DURATION_EMPTY
import task.queries.GetTaskByProjectIdAndTimeDetailsQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class Validator[C <: Command](taskAggregate: TaskAggregate, projectAggregate: ProjectAggregate) {
  def validate(command: C): Future[Either[Error, C]]

  val isProjectEmpty: C => Either[Error, C] =
    (command: C) => if (command.projectId.isBlank) Left(EmptyProjectId) else Right(command)

  val projectExist: Future[Either[Error, C]] => Future[Either[Error, C]] =
    (result: Future[Either[Error, C]]) => result.flatMap {
      case Left(error) => Future.successful(Left(error))
      case Right(command) => projectExist(command)
    }

  val isProperStartDate: Either[Error, C] => Either[Error, C] = {
    case Left(error) => Left(error)
    case Right(command: C with StartDate) => checkStartDate(command)
  }

  private def checkStartDate(command: C with StartDate) = {
    if (NIL_LOCAL_DATE_TIME.equals(command.getStart))
      Left(IncorrectDate) else Right(command)
  }

  val isProperDuration: Either[Error, C] => Either[Error, C] = {
    case Left(error) => Left(error)
    case Right(command: C with WithTimeDetails) => checkDuration(command)
  }

  val isNotInConflict: Either[Error, C] => Future[Either[Error, C]] = {
    case Left(error) => Future.successful(Left(error))
    case Right(command: C) => checkIfTaskIsInConflict(command)
  }

  private def checkDuration(command: C with WithTimeDetails) = {
    if (TASK_DURATION_EMPTY.equals(command.getDuration))
      Left(IncorrectDuration) else Right(command)
  }

  private def projectExist(command: C) = {
    val query = GetProjectByIdAndAuthorIdQuery(command.projectId, command.authorId)
    projectAggregate.getProject(query)
      .map {
        case None => Left(ProjectIdNotFound)
        case Some(_) => Right(command)
      }
  }

  private def checkIfTaskIsInConflict(command: C): Future[Either[Error, C]] = {
    val commandWithTimeDetails = command.asInstanceOf[WithTimeDetails]
    val query = GetTaskByProjectIdAndTimeDetailsQuery(commandWithTimeDetails.getProjectId, commandWithTimeDetails.getTimeDetails)
    taskAggregate.getTask(query).map {
      case Some(_) => Left(TaskInConflictWithAnother)
      case None => Right(command)
    }
  }
}

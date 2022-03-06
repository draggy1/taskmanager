package task.validators

import authentication.{Error, TaskInConflictWithAnother, TaskToDeleteAlreadyDeleted, TaskToDeleteNotExist}
import common.{Command, WithStart, WithTaskTimeDetails}
import task.TaskAggregate
import task.queries.{GetTaskByProjectIdAndStartQuery, GetTaskByProjectIdAndTimeDetailsQuery}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class TaskValidator[C <: Command](aggregate: TaskAggregate) {
  def checkIfTaskIsInConflict(command: C with WithTaskTimeDetails): Future[Either[Error, C]] = {
    aggregate.getTask(GetTaskByProjectIdAndTimeDetailsQuery(command.getProjectId, command.getTimeDetails))
      .map {
        case Some(_) => Left(TaskInConflictWithAnother)
        case None => Right(command)
    }
  }

  def isProvidedTaskExist(command: C with WithStart): Future[Either[Error, C]] = {
    aggregate.getTask(GetTaskByProjectIdAndStartQuery(command.getProjectId, command.getStart))
      .map {
        case None => Left(TaskToDeleteNotExist)
        case Some(_) => Right(command)
      }
  }

  def isTaskAlreadyDeleted(command: C with WithStart): Future[Either[Error, C]] =
    aggregate.getTask(GetTaskByProjectIdAndStartQuery(command.getProjectId, command.getStart))
      .map {
        case Some(task) => if (task.taskTimeDetails.delete.isEmpty) Right(command) else Left(TaskToDeleteAlreadyDeleted)
    }
}

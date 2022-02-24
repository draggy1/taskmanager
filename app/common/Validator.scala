package common

import authentication.{DuplicatedProjectId, EmptyAuthorId, EmptyProjectId, Error, IncorrectDate, IncorrectDuration, ProjectIdNotFound, ProjectToDeleteAlreadyDeleted, TaskInConflictWithAnother, TaskToDeleteAlreadyDeleted, TaskToDeleteNotExist, UserIsNotAuthor}
import project.ProjectAggregate
import project.queries.GetProjectByIdQuery
import task.TaskAggregate
import task.TaskDuration.TASK_DURATION_EMPTY
import task.queries.{GetTaskByProjectIdAndStartQuery, GetTaskByProjectIdAndTimeDetailsQuery}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Validator [C <: Command](projectAggregate: ProjectAggregate, taskAggregate: TaskAggregate){
  def isProjectIdBlank(command: C): Either[EmptyProjectId.type, C] =
    if (command.isProjectIdBlank) Left(EmptyProjectId) else Right(command)

  def isAuthorIdBlank(command: C): Either[EmptyAuthorId.type, C] = {
    if (command.isAuthorIdBlank) Left(EmptyAuthorId) else Right(command)
  }

  def isProjectDuplicated(command: C): Future[Either[DuplicatedProjectId.type, C]] =
    projectAggregate.getProject(GetProjectByIdQuery(command.getProjectId))
      .map {
        case Some(_) => Left(DuplicatedProjectId)
        case None => Right(command)
      }

  def isProjectExist(command: C): Future[Either[ProjectIdNotFound.type, C]] =
    projectAggregate.getProject(GetProjectByIdQuery(command.getProjectToCheckIfExist))
      .map {
        case None => Left(ProjectIdNotFound)
        case Some(_) => Right(command)
      }

  def isUserAuthorOfProject(command: C): Future[Either[Error, C]] =
    projectAggregate.getProject(GetProjectByIdQuery(command.getProjectToCheckIfExist))
      .map {
        case Some(project) => if(project.authorId.equals(command.getAuthorId))
          Right(command) else Left(UserIsNotAuthor)
      }

  def isProjectAlreadyDeleted(command: C): Future[Either[Error, C]] =
    projectAggregate.getProject(GetProjectByIdQuery(command.getProjectId)).map {
      case Some(project) => if(project.deleted.isEmpty) Right(command) else Left(ProjectToDeleteAlreadyDeleted)
    }

  def isDurationEmpty(command: C with WithTaskTimeDetails): Either[IncorrectDuration.type, C with WithTaskTimeDetails] = {
    if (TASK_DURATION_EMPTY.equals(command.getTimeDetails.duration))
      Left(IncorrectDuration) else Right(command)
  }

  def isStartDateIncorrect(command: C with WithStart): Either[IncorrectDate.type, C with WithStart] = {
    if (command.isStartDateNotCorrect) Left(IncorrectDate) else Right(command)
  }

  def checkIfTaskIsInConflict(command: C with WithTaskTimeDetails): Future[Either[Error, C]] = {
    val query = GetTaskByProjectIdAndTimeDetailsQuery(command.getProjectId, command.getTimeDetails)
    taskAggregate.getTask(query).map {
      case Some(_) => Left(TaskInConflictWithAnother)
      case None => Right(command)
    }
  }

  def isProvidedTaskExist(command: C with WithStart): Future[Either[Error, C]] =
    taskAggregate.getTask(GetTaskByProjectIdAndStartQuery(command.getProjectId, command.getStart))
      .map {
        case None => Left(TaskToDeleteNotExist)
        case Some(_) => Right(command)
      }

  def isTaskAlreadyDeleted(command: C with WithStart): Future[Either[Error, C]] =
    taskAggregate.getTask(GetTaskByProjectIdAndStartQuery(command.getProjectId, command.getStart)).map {
      case Some(task) => if (task.taskTimeDetails.delete.isEmpty) Right(command) else Left(TaskToDeleteAlreadyDeleted)
    }
}

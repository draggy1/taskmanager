package common

import authentication.{DuplicatedProjectId, EmptyAuthorId, EmptyProjectId, Error, IncorrectDate, IncorrectDuration, ProjectIdNotFound, ProjectToDeleteAlreadyDeleted, TaskInConflictWithAnother, TaskToDeleteAlreadyDeleted, TaskToDeleteNotExist, UserIsNotAuthor}
import project.ProjectAggregate
import project.queries.GetProjectByIdQuery
import task.TaskAggregate
import task.TaskDuration.TASK_DURATION_EMPTY
import task.queries.{GetTaskByProjectIdAndStartQuery, GetTaskByProjectIdAndTimeDetailsQuery}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CommonValidators[C <: Command](projectAggregate: ProjectAggregate, taskAggregate: TaskAggregate) {
  val isProjectEmpty: C => Either[Error, C] =
    (command: C) => {
      if (command.isProjectIdBlank) Left(EmptyProjectId) else Right(command)
    }

  val notValidAuthorId: Either[Error, C] => Either[Error, C] = {
    case Left(error) => Left(error)
    case Right(command) => if (command.isAuthorIdBlank) Left(EmptyAuthorId) else Right(command)
  }

  val isDuplicated: Either[Error, C] => Future[Either[Error, C]] = {
    case Left(error) => Future.successful(Left(error))
    case Right(command) => isDuplicated(command)
  }

  val mapToFuture: Either[Error, C] => Future[Either[Error, C]] = either => Future.successful(either)

  private def isDuplicated(command: C) =
    projectAggregate.getProject(GetProjectByIdQuery(command.getProjectId))
      .map {
        case Some(_) => Left(DuplicatedProjectId)
        case None => Right(command)
      }

  val isProjectExist: Future[Either[Error, C]] => Future[Either[Error, C]] =
    (result: Future[Either[Error, C]]) => result.flatMap {
      case Left(error) => Future.successful(Left(error))
      case Right(command) => isProjectExist(command)
    }

  private def isProjectExist(command: C) = {
    projectAggregate.getProject(GetProjectByIdQuery(command.getProjectToCheckIfExist))
      .map {
        case None => Left(ProjectIdNotFound)
        case Some(_) => Right(command)
      }
  }

  val userIsNotAuthor: Future[Either[Error, C]] => Future[Either[Error, C]] =
    (result: Future[Either[Error, C]]) => result.flatMap {
      case Left(error) => Future.successful(Left(error))
      case Right(command) => isUserAuthorOfProject(command)
    }

  private def isUserAuthorOfProject(command: C): Future[Either[Error, C]] = {
    projectAggregate.getProject(GetProjectByIdQuery(command.getProjectToCheckIfExist))
      .map {
        case Some(project) => if(project.authorId.equals(command.getAuthorId))
          Right(command) else Left(UserIsNotAuthor)
      }
  }

  val isProjectAlreadyDeleted: Future[Either[Error, C]] => Future[Either[Error, C]] =
    (result: Future[Either[Error, C]]) => result.flatMap {
      case Left(error) => Future.successful(Left(error))
      case Right(command) => isProjectAlreadyDeleted(command)
    }

  private def isProjectAlreadyDeleted(command: C): Future[Either[Error, C]] =
    projectAggregate.getProject(GetProjectByIdQuery(command.getProjectId)).map {
      case Some(project) => if(project.deleted.isEmpty) Right(command) else Left(ProjectToDeleteAlreadyDeleted)
    }

  val isProperDuration: Either[Error, C with WithTaskTimeDetails] => Either[Error, C] = {
    case Left(error) => Left(error)
    case Right(command) => if(TASK_DURATION_EMPTY.equals(command.getTimeDetails.duration))
      Left(IncorrectDuration) else Right(command)
  }

  val isStartDateCorrect: Either[Error, C with WithStart] => Either[Error, C] = {
    case Left(error) => Left(error)
    case Right(command: C with WithStart) => if(command.isStartDateNotCorrect)
      Left(IncorrectDate) else Right(command)
  }

  val isNotInConflict: Either[Error, C with WithTaskTimeDetails] => Future[Either[Error, C]] = {
    case Left(error) => Future.successful(Left(error))
    case Right(command: C with WithTaskTimeDetails) => checkIfTaskIsInConflict(command)
  }

  private def checkIfTaskIsInConflict(command: C with WithTaskTimeDetails): Future[Either[Error, C]] = {
    val query = GetTaskByProjectIdAndTimeDetailsQuery(command.getProjectId, command.getTimeDetails)
    taskAggregate.getTask(query).map {
      case Some(_) => Left(TaskInConflictWithAnother)
      case None => Right(command)
    }
  }

  val isProvidedTaskExist: Either[Error, C with WithStart] => Future[Either[Error, C]] = {
    case Left(error) => Future.successful(Left(error))
    case Right(command) => isProvidedTaskExist(command)
  }

  private def isProvidedTaskExist(command: C with WithStart): Future[Either[Error, C]] = {
    taskAggregate.getTask(GetTaskByProjectIdAndStartQuery(command.getProjectId, command.getStart))
      .map {
        case None => Left(TaskToDeleteNotExist)
        case Some(_) => Right(command)
      }
  }

  val isTaskAlreadyDeleted: Future[Either[Error, C with WithStart]] => Future[Either[Error, C]] =
    (result: Future[Either[Error, C with WithStart]]) => {
      result.flatMap{
        case Left(error) => Future.successful(Left(error))
        case Right(command) => isTaskAlreadyDeleted(command)
      }
    }

  private def isTaskAlreadyDeleted(command: C with WithStart): Future[Either[Error, C]] =
    taskAggregate.getTask(GetTaskByProjectIdAndStartQuery(command.getProjectId, command.getStart)).map {
      case Some(task) => if (task.taskTimeDetails.delete.isEmpty) Right(command) else Left(TaskToDeleteAlreadyDeleted)
    }
}

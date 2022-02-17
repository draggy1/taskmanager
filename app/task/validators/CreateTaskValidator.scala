package task.validators

import authentication.{EmptyProjectId, Error, IncorrectDate, IncorrectDuration, ProjectIdNotFound, TaskInConflictWithAnother}
import common.TimeUtils.NIL_LOCAL_DATE_TIME
import common.ValidationContext
import project.ProjectAggregate
import project.queries.GetProjectByIdAndAuthorIdQuery
import task.TaskAggregate
import task.TaskDuration.TASK_DURATION_EMPTY
import task.commands.CreateTaskCommand
import task.queries.GetTaskByProjectIdAndTimeDetailsQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CreateTaskValidator(taskAggregate: TaskAggregate, projectAggregate: ProjectAggregate) {

 def validate(command: CreateTaskCommand): Future[Either[Error, CreateTaskCommand]] = {
   val project = projectAggregate.getProject(GetProjectByIdAndAuthorIdQuery(command.projectId, command.authorId))
   val task = taskAggregate.getTask(GetTaskByProjectIdAndTimeDetailsQuery(command.getProjectId, command.getTimeDetails))

   isProjectEmpty
     .andThen(isProperStartDate)
     .andThen(isProperDuration)
     .andThen(isNotInConflict)
     .andThen(projectExist)
     .apply(ValidationContext(command, project, task))
     .map {
       case Left(error) => Left(error)
       case Right(context) => Right(context.command)
     }
 }

  val isProjectEmpty: ValidationContext[CreateTaskCommand] => Either[Error, ValidationContext[CreateTaskCommand]] =
    (context: ValidationContext[CreateTaskCommand]) => if (context.command.projectId.isBlank) Left(EmptyProjectId) else Right(context)

  val projectExist: Future[Either[Error, ValidationContext[CreateTaskCommand]]] => Future[Either[Error, ValidationContext[CreateTaskCommand]]] =
    (result: Future[Either[Error, ValidationContext[CreateTaskCommand]]]) => result.flatMap {
      case Left(error) => Future.successful(Left(error))
      case Right(context) => projectExist(context)
    }

  val isProperStartDate: Either[Error, ValidationContext[CreateTaskCommand]] => Either[Error, ValidationContext[CreateTaskCommand]] = {
    case Left(error) => Left(error)
    case Right(context) => checkStartDate(context)
  }

  private def checkStartDate(context: ValidationContext[CreateTaskCommand]) = {
    if (NIL_LOCAL_DATE_TIME.equals(context.command.getStart))
      Left(IncorrectDate) else Right(context)
  }

  val isProperDuration: Either[Error, ValidationContext[CreateTaskCommand]] => Either[Error, ValidationContext[CreateTaskCommand]] = {
    case Left(error) => Left(error)
    case Right(context) => checkDuration(context)
  }

  val isNotInConflict: Either[Error, ValidationContext[CreateTaskCommand]] => Future[Either[Error, ValidationContext[CreateTaskCommand]]] = {
    case Left(error) => Future.successful(Left(error))
    case Right(context) => checkIfTaskIsInConflict(context)
  }

  private def checkDuration(context: ValidationContext[CreateTaskCommand]) = {
    if (TASK_DURATION_EMPTY.equals(context.command.getDuration))
      Left(IncorrectDuration) else Right(context)
  }

  private def projectExist(context: ValidationContext[CreateTaskCommand]) = {
    context.project
      .map {
        case None => Left(ProjectIdNotFound)
        case Some(_) => Right(context)
      }
  }

  private def checkIfTaskIsInConflict(context: ValidationContext[CreateTaskCommand]): Future[Either[Error, ValidationContext[CreateTaskCommand]]] = context.task.map {
    case Some(_) => Left(TaskInConflictWithAnother)
    case None => Right(context)
  }
}

object CreateTaskValidator {
  def apply(taskAggregate: TaskAggregate, projectAggregate: ProjectAggregate): CreateTaskValidator =
    new CreateTaskValidator(taskAggregate, projectAggregate)
}

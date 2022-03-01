package common.validators

import authentication.Error
import common.{Command, WithStart, WithTaskTimeDetails}
import project.ProjectAggregate
import project.validators.ProjectValidator
import task.TaskAggregate
import task.validators.TaskValidator

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ValidatorFacade[C <: Command](projectAggregate: ProjectAggregate, taskAggregate: TaskAggregate) {
  private val commonValidator: CommonValidator[C] = CommonValidator[C]()
  private val projectValidator: ProjectValidator[C] = ProjectValidator[C](projectAggregate)
  private val taskValidator: TaskValidator[C] = TaskValidator[C](taskAggregate)

  val isProjectIdBlank: C => Either[Error, C] = (command: C) => commonValidator.isProjectIdBlank(command)

  val isAuthorIdBlank: Either[Error, C] => Either[Error, C] = {
    case Left(error) => Left(error)
    case Right(command) => commonValidator.isAuthorIdBlank(command)
  }

  val isProjectDuplicated: Either[Error, C] => Future[Either[Error, C]] = {
    case Left(error) => Future.successful(Left(error))
    case Right(command) => projectValidator.isProjectDuplicated(command)
  }

  val mapToFuture: Either[Error, C] => Future[Either[Error, C]] = either => Future.successful(either)

  val isProjectExist: Future[Either[Error, C]] => Future[Either[Error, C]] =
    (result: Future[Either[Error, C]]) => result.flatMap {
      case Left(error) => Future.successful(Left(error))
      case Right(command) => projectValidator.isProjectExist(command)
    }

  val userIsNotAuthor: Future[Either[Error, C]] => Future[Either[Error, C]] =
    (result: Future[Either[Error, C]]) => result.flatMap {
      case Left(error) => Future.successful(Left(error))
      case Right(command) => projectValidator.isUserAuthorOfProject(command)
    }

  val isProjectAlreadyDeleted: Future[Either[Error, C]] => Future[Either[Error, C]] =
    (result: Future[Either[Error, C]]) => result.flatMap {
      case Left(error) => Future.successful(Left(error))
      case Right(command) => projectValidator.isProjectAlreadyDeleted(command)
    }

  val isDurationEmpty: Either[Error, C with WithTaskTimeDetails] => Either[Error, C] = {
    case Left(error) => Left(error)
    case Right(command) => commonValidator.isDurationEmpty(command)
  }

  val isStartDateCorrect: Either[Error, C with WithStart] => Either[Error, C] = {
    case Left(error) => Left(error)
    case Right(command: C with WithStart) => commonValidator.isStartDateIncorrect(command)
  }

  val isNotInConflict: Either[Error, C with WithTaskTimeDetails] => Future[Either[Error, C]] = {
    case Left(error) => Future.successful(Left(error))
    case Right(command: C with WithTaskTimeDetails) => taskValidator.checkIfTaskIsInConflict(command)
  }

  val isProvidedTaskExist: Either[Error, C with WithStart] => Future[Either[Error, C]] = {
    case Left(error) => Future.successful(Left(error))
    case Right(command) => taskValidator.isProvidedTaskExist(command)
  }

  val isTaskAlreadyDeleted: Future[Either[Error, C with WithStart]] => Future[Either[Error, C]] =
    (result: Future[Either[Error, C with WithStart]]) => {
      result.flatMap{
        case Left(error) => Future.successful(Left(error))
        case Right(command) => taskValidator.isTaskAlreadyDeleted(command)
      }
    }
}

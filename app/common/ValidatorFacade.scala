package common

import authentication.Error
import project.ProjectAggregate
import task.TaskAggregate

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ValidatorFacade[C <: Command](projectAggregate: ProjectAggregate, taskAggregate: TaskAggregate) {
  val validator: Validator[C] = new Validator[C](projectAggregate, taskAggregate)

  val isProjectIdBlank: C => Either[Error, C] = (command: C) => validator.isProjectIdBlank(command)

  val isAuthorIdBlank: Either[Error, C] => Either[Error, C] = {
    case Left(error) => Left(error)
    case Right(command) => validator.isAuthorIdBlank(command)
  }

  val isProjectDuplicated: Either[Error, C] => Future[Either[Error, C]] = {
    case Left(error) => Future.successful(Left(error))
    case Right(command) => validator.isProjectDuplicated(command)
  }

  val mapToFuture: Either[Error, C] => Future[Either[Error, C]] = either => Future.successful(either)

  val isProjectExist: Future[Either[Error, C]] => Future[Either[Error, C]] =
    (result: Future[Either[Error, C]]) => result.flatMap {
      case Left(error) => Future.successful(Left(error))
      case Right(command) => validator.isProjectExist(command)
    }

  val userIsNotAuthor: Future[Either[Error, C]] => Future[Either[Error, C]] =
    (result: Future[Either[Error, C]]) => result.flatMap {
      case Left(error) => Future.successful(Left(error))
      case Right(command) => validator.isUserAuthorOfProject(command)
    }

  val isProjectAlreadyDeleted: Future[Either[Error, C]] => Future[Either[Error, C]] =
    (result: Future[Either[Error, C]]) => result.flatMap {
      case Left(error) => Future.successful(Left(error))
      case Right(command) => validator.isProjectAlreadyDeleted(command)
    }

  val isDurationEmpty: Either[Error, C with WithTaskTimeDetails] => Either[Error, C] = {
    case Left(error) => Left(error)
    case Right(command) => validator.isDurationEmpty(command)
  }

  val isStartDateCorrect: Either[Error, C with WithStart] => Either[Error, C] = {
    case Left(error) => Left(error)
    case Right(command: C with WithStart) => validator.isStartDateIncorrect(command)
  }

  val isNotInConflict: Either[Error, C with WithTaskTimeDetails] => Future[Either[Error, C]] = {
    case Left(error) => Future.successful(Left(error))
    case Right(command: C with WithTaskTimeDetails) => validator.checkIfTaskIsInConflict(command)
  }

  val isProvidedTaskExist: Either[Error, C with WithStart] => Future[Either[Error, C]] = {
    case Left(error) => Future.successful(Left(error))
    case Right(command) => validator.isProvidedTaskExist(command)
  }

  val isTaskAlreadyDeleted: Future[Either[Error, C with WithStart]] => Future[Either[Error, C]] =
    (result: Future[Either[Error, C with WithStart]]) => {
      result.flatMap{
        case Left(error) => Future.successful(Left(error))
        case Right(command) => validator.isTaskAlreadyDeleted(command)
      }
    }
}

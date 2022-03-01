package common.validators

import authentication.{EmptyAuthorId, EmptyProjectId, IncorrectDate, IncorrectDuration}
import common.{Command, WithStart, WithTaskTimeDetails}
import task.TaskDuration.TASK_DURATION_EMPTY

case class CommonValidator[C <: Command]() {
  def isProjectIdBlank(command: C): Either[EmptyProjectId.type, C] =
    if (command.isProjectIdBlank) Left(EmptyProjectId) else Right(command)

  def isAuthorIdBlank(command: C): Either[EmptyAuthorId.type, C] = {
    if (command.isAuthorIdBlank) Left(EmptyAuthorId) else Right(command)
  }

  def isDurationEmpty(command: C with WithTaskTimeDetails): Either[IncorrectDuration.type, C with WithTaskTimeDetails] = {
    if (TASK_DURATION_EMPTY.equals(command.getTimeDetails.duration))
      Left(IncorrectDuration) else Right(command)
  }

  def isStartDateIncorrect(command: C with WithStart): Either[IncorrectDate.type, C with WithStart] = {
    if (command.isStartDateNotCorrect) Left(IncorrectDate) else Right(command)
  }
}

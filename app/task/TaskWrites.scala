package task

import play.api.libs.json.{Json, Writes}
import project.commands.DeleteProjectCommand
import task.commands.{CreateTaskCommand, DeleteTaskCommand, UpdateTaskCommand}

object TaskWrites {
  implicit val taskDuration: Writes[TaskDuration] = (taskDuration: TaskDuration) => Json.obj(
    "hours" -> taskDuration.hoursValue,
    "minutes" -> taskDuration.minutesValue)

  implicit val timeDetailsWrites: Writes[TaskTimeDetails] = (taskTimeDetails: TaskTimeDetails) => Json.obj(
    "start_date" -> taskTimeDetails.start,
    "duration" -> taskTimeDetails.duration)

  implicit val taskWrites: Writes[CreateTaskCommand] = (command: CreateTaskCommand) => Json.obj(
    "project_id" -> command.projectId,
    "author_id" -> command.authorId,
    "task_time_details" -> command.taskTimeDetails,
    "volume" -> command.volume,
    "comment" -> command.comment)

  implicit val taskDeleteWrites: Writes[DeleteTaskCommand] = (command: DeleteTaskCommand) => Json.obj(
    "project_id" -> command.projectId,
    "author_id" -> command.authorId,
    "start_date" -> command.start)

  implicit val taskUpdateWrites: Writes[UpdateTaskCommand] = (command: UpdateTaskCommand) => Json.obj(
    "project_id_old" -> command.projectIdOld,
    "project_id_new"-> command.projectIdNew,
    "author_id_old" -> command.authorIdOld,
    "author_id_new" -> command.authorIdNew,
    "start_date_old" -> command.startDateOld,
    "start_date_new" -> command.taskTimeDetails.start,
    "duration" -> command.taskTimeDetails.duration,
    "volume" -> command.volume,
    "comment" -> command.comment)

  implicit val deleteResponseWrites: Writes[DeleteProjectCommand] = (data: DeleteProjectCommand) => Json.obj(
    "author_id" -> data.authorId,
    "project_id" -> data.projectId)
}

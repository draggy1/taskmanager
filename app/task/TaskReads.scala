package task

import common.utils.StringUtils.EMPTY
import common.utils.TimeUtils.{mapToLocalDateTime, prepareTaskTimeDetails}
import common.utils.UUIDUtils.UUID_NIL
import common.utils.TimeUtils
import io.jvm.uuid.UUID
import play.api.libs.functional.syntax.{toAlternativeOps, toFunctionalBuilderOps}
import play.api.libs.json.{JsPath, Reads}
import task.commands.{CreateTaskCommand, DeleteTaskCommand, UpdateTaskCommand}

object TaskReads {
  implicit val createTaskCommandReads: Reads[CreateTaskCommand] =
    (((JsPath \ "project_id").read[String] or Reads.pure(EMPTY)) and
      ((JsPath \ "author_id").read[UUID] or Reads.pure(UUID_NIL)) and
      ((JsPath \ "start_date").read[String] or Reads.pure(EMPTY)) and
      (JsPath \ "duration").readNullable[String] and
      (JsPath \ "volume").readNullable[Int] and
      (JsPath \ "comment").readNullable[String]) ((projectId, authorId, startDate, duration, volume, comment) =>
      CreateTaskCommand(projectId, authorId, TimeUtils.prepareTaskTimeDetails(startDate, duration), volume, comment))

  implicit val deleteTaskCommandReads: Reads[DeleteTaskCommand] =
    (((JsPath \ "project_id").read[String] or Reads.pure(EMPTY)) and
      ((JsPath \ "author_id").read[UUID] or Reads.pure(UUID_NIL)) and
      ((JsPath \ "start_date").read[String] or Reads.pure(EMPTY))
      ) ((projectId, authorId, startDate) => DeleteTaskCommand(projectId, authorId, mapToLocalDateTime(startDate)))

  implicit val updateTaskCommandReads: Reads[UpdateTaskCommand] =
    (((JsPath \ "project_id_old").read[String] or Reads.pure(EMPTY)) and
      ((JsPath \ "project_id_new").read[String] or Reads.pure(EMPTY)) and
      ((JsPath \ "author_id_old").read[UUID] or Reads.pure(UUID_NIL)) and
      ((JsPath \ "author_id_new").read[UUID] or Reads.pure(UUID_NIL)) and
      ((JsPath \ "start_date_old").read[String] or Reads.pure(EMPTY)) and
      ((JsPath \ "start_date_new").read[String] or Reads.pure(EMPTY)) and
      (JsPath \ "duration").readNullable[String] and
      (JsPath \ "volume").readNullable[Int] and
      (JsPath \ "comment").readNullable[String]
      ) ((projectIdOld, projectIdNew, authorIdOld, authorIdNew, startDateOld, startDateNew, duration, volume, comment) =>
      UpdateTaskCommand(projectIdOld,
        projectIdNew,
        authorIdOld,
        authorIdNew,
        mapToLocalDateTime(startDateOld),
        prepareTaskTimeDetails(startDateNew, duration),
        volume,
        comment))
}

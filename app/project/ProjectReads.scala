package project

import common.StringUtils.EMPTY
import common.UUIDUtils.UUID_NIL
import io.jvm.uuid.UUID
import play.api.libs.functional.syntax.{toAlternativeOps, toFunctionalBuilderOps}
import play.api.libs.json.{JsPath, Reads}
import project.commands.{CreateProjectCommand, DeleteProjectCommand, UpdateProjectCommand}

object ProjectReads {
  implicit val createProjectCommandReads: Reads[CreateProjectCommand] =
    ((JsPath \ "author_id").read[UUID] or Reads.pure(UUID_NIL) and
      ((JsPath \ "project_id").read[String] or Reads.pure(EMPTY)))(
      (authorId, projectId) => CreateProjectCommand(authorId, projectId))

  implicit val updateProjectCommandReads: Reads[UpdateProjectCommand] =
    ((JsPath \ "author_id").read[UUID] or Reads.pure(UUID_NIL) and
      ((JsPath \ "project_id_old").read[String] or Reads.pure(EMPTY)) and
      ((JsPath \ "project_id_new").read[String] or Reads.pure(EMPTY))
      )((authorId, projectIdOld, projectIdNew) => UpdateProjectCommand(authorId, projectIdOld, projectIdNew))

  implicit val deleteProjectCommandReads: Reads[DeleteProjectCommand] =
    ((JsPath \ "author_id").read[UUID] or Reads.pure(UUID_NIL) and
      ((JsPath \ "project_id").read[String] or Reads.pure(EMPTY)))(
      (authorId, projectId) => DeleteProjectCommand(authorId, projectId))
}


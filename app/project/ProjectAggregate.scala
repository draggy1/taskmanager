package project


import common.StringUtils
import common.responses.Response
import common.responses.Response.getResult
import play.api.http.Status
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.{ResponseHeader, Result}
import project.commands.{CreateProjectCommand, DeleteProjectCommand, UpdateProjectCommand}
import project.queries.{GetProjectByIdAndAuthorIdQuery, GetProjectByIdQuery}
import task.TaskRepository

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ProjectAggregate @Inject()(projectRepository: ProjectRepository, taskRepository: TaskRepository){
  private implicit val projectWrites: Writes[DeleteProjectCommand] = (data: DeleteProjectCommand) => Json.obj(
    "author_id" -> data.authorId,
    "project_id" -> data.projectId)

  def createProject(command: CreateProjectCommand): Future[Result] =
    projectRepository.create(command)

  def updateProject(command: UpdateProjectCommand): Future[Result] =
    projectRepository.update(command)

  def getProject(query: GetProjectByIdQuery): Future[Option[Project]] =
    projectRepository.find(query.projectId)

  def getProject(query: GetProjectByIdAndAuthorIdQuery): Future[Option[Project]] =
     projectRepository.find(query.projectId, query.authorId)

  def delete(command: DeleteProjectCommand): Future[Result] = {
    for {
      deleteResult <- projectRepository.delete(command)
      createResult <- taskRepository.deleteAll(command.projectId)
    } yield mergeFutures(deleteResult, createResult, command)
  }

  def mergeFutures(deleteResult: Result, createResult: Result, command: DeleteProjectCommand): Result = {
    if (areStatusesSuccessful(deleteResult, createResult)){
      val json = Json.toJson(Response[DeleteProjectCommand](success = true, "Project deleted", command))
      prepareResult(json, OK)
    } else {
      val json = Json.toJson(Response[String](success = false, "Project delete failed", StringUtils.EMPTY))
      prepareResult(json, INTERNAL_SERVER_ERROR)
    }
  }
  private def prepareResult(json: JsValue, status: Int): Result = {
    getResult(ResponseHeader(status), json)
  }

  private def areStatusesSuccessful(deleteResult: Result, createResult: Result) =
    deleteResult.header.status == OK && createResult.header.status == Status.OK
}

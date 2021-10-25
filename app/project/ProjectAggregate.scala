package project

import play.api.mvc.Result
import project.commands.{CreateProjectCommand, UpdateProjectCommand}
import project.queries.GetProjectByIdQuery

import javax.inject.Inject
import scala.concurrent.Future

class ProjectAggregate @Inject()(val repository: ProjectRepository){
  def createProject(command: CreateProjectCommand): Future[Result] = {
    repository.create(command)
  }

  def updateProject(command: UpdateProjectCommand): Future[Result] = {
    repository.updateProjectId(command)
  }

  def getProject(query: GetProjectByIdQuery): Future[Option[Project]] =
    repository.find(query.projectId)
}

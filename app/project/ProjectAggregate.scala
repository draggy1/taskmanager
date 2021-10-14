package project

import org.mongodb.scala.bson.ObjectId
import play.api.mvc.Result
import project.commands.CreateProjectCommand
import project.queries.GetProjectByIdQuery

import java.time.LocalDateTime
import javax.inject.Inject
import scala.concurrent.Future

class ProjectAggregate @Inject()(val repository: ProjectRepository){
  def createProject(command: CreateProjectCommand): Future[Result] = {
    val project = Project(new ObjectId(), command.userId, command.projectId, LocalDateTime.now())
    repository.create(project)
  }

  def getProject(query: GetProjectByIdQuery): Future[Option[Project]] =
    repository.find(query.projectId)
}

package project

import authentication.{DuplicatedProjectId, Error}
import project.commands.CreateProjectCommand
import project.queries.GetProjectByIdQuery

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class ProjectValidator @Inject()(aggregate: ProjectAggregate) {
  def validate(command: CreateProjectCommand): Future[Either[Error, CreateProjectCommand]] =
    aggregate.getProject(GetProjectByIdQuery(command.projectId))
      .map {
        case Some(_) => Left(DuplicatedProjectId)
        case None => Right(command)
      }
}

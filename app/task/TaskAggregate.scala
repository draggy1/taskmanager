package task

import play.api.mvc.Result
import task.commands.CreateTaskCommand
import task.queries.{GetTaskByProjectIdAndTimeDetailsQuery, GetTaskByProjectIdQuery}

import javax.inject.Inject
import scala.concurrent.Future

class TaskAggregate @Inject()(val repository: TaskRepository){
  def createTask(command: CreateTaskCommand): Future[Result] = repository.create(command)

  def getTask(query: GetTaskByProjectIdAndTimeDetailsQuery): Future[Option[Task]] =
    repository.find(query)

  def getTask(query: GetTaskByProjectIdQuery): Future[Option[Task]] = repository.find(query)
}

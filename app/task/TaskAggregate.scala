package task

import play.api.mvc.Result
import task.commands.{CreateTaskCommand, DeleteTaskCommand, UpdateTaskCommand}
import task.queries.{GetTaskByProjectIdAndStartQuery, GetTaskByProjectIdAndTimeDetailsQuery}

import javax.inject.Inject
import scala.concurrent.Future

class TaskAggregate @Inject()(val repository: TaskRepository){
  def createTask(command: CreateTaskCommand): Future[Result] = repository.create(command)

  def getTask(query: GetTaskByProjectIdAndTimeDetailsQuery): Future[Option[Task]] = repository.find(query)

  def getTask(query: GetTaskByProjectIdAndStartQuery): Future[Option[Task]] = repository.find(query)

  def deleteTask(command: DeleteTaskCommand): Future[Result] = repository.delete(command)

  def updateTask(command: UpdateTaskCommand): Future[Result] = repository.update(command)
}

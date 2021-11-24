package task

import play.api.mvc.Result
import task.commands.CreateTaskCommand

import javax.inject.Inject
import scala.concurrent.Future

class TaskAggregate @Inject()(val repository: TaskRepository){
  def createTask(command: CreateTaskCommand): Future[Result] = repository.create(command)
}

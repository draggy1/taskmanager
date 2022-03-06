package task

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.{and, or}
import common.Command
import common.responses.Response
import common.responses.Response.{getResult, getSuccessResult}
import common.utils.StringUtils.EMPTY
import configuration.MongoDbManager
import org.bson.conversions.Bson
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.model.Filters.{equal, gt, lt}
import org.mongodb.scala.model.Updates.{combine, set}
import play.api.http.Status.{CREATED, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{Json, Writes}
import play.api.mvc.{ResponseHeader, Result}
import project.commands.DeleteProjectCommand
import task.TaskWrites.{deleteResponseWrites, taskDeleteWrites, taskUpdateWrites, taskWrites}
import task.commands.{CreateTaskCommand, DeleteTaskCommand, UpdateTaskCommand}
import task.queries.{GetTaskByProjectIdAndStartQuery, GetTaskByProjectIdAndTimeDetailsQuery, GetTaskByProjectIdQuery}

import java.time.LocalDateTime
import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class TaskRepository @Inject()(config: MongoDbManager){
  private val collection: MongoCollection[Task] = config.database
    .withCodecRegistry(config.taskCodecRegistry)
    .getCollection("task")

  def create(command: CreateTaskCommand): Future[Result] = {
    val task =
      Task(new ObjectId(), command.projectId, command.authorId, command.taskTimeDetails, command.volume, command.comment)

    collection.insertOne(task)
      .toFuture()
      .map(_ => getSuccessResult[CreateTaskCommand](CREATED, "Task created", command))
      .recover { case _ => prepareErrorResult() }
  }

  def find(query: GetTaskByProjectIdAndTimeDetailsQuery): Future[Option[Task]] = {
    val taskStart = query.taskTimeDetails.start
    val taskEnd = query.taskTimeDetails.end

    val equalProjectId = equal("projectId", query.projectId)
    val oldStartBetweenNewTask = and(gt("taskTimeDetails.start", taskStart), lt("taskTimeDetails.start", taskEnd))
    val oldEndBetweenNewTask = and(gt("taskTimeDetails.end", taskStart), lt("taskTimeDetails.end", taskEnd))
    val oldTaskContainsNewTask = and(lt("taskTimeDetails.start", taskStart), gt("taskTimeDetails.end", taskEnd))

    collection.find(and(equalProjectId, or(oldStartBetweenNewTask, oldEndBetweenNewTask, oldTaskContainsNewTask)))
      .first()
      .toFutureOption()
  }

  def find(query: GetTaskByProjectIdQuery): Future[Option[Task]] = {
    val equalProjectId = equal("projectId", query.projectId)

    collection.find(equalProjectId)
      .first()
      .toFutureOption()
  }

  def find(query: GetTaskByProjectIdAndStartQuery): Future[Option[Task]] = {
    val equalProjectId = equal("projectId", query.projectId)
    val equalStart = equal("taskTimeDetails.start", query.start)

    collection.find(and(equalProjectId, equalStart))
      .first()
      .toFutureOption()
  }

  def deleteOne(command: DeleteTaskCommand): Future[Result] = {
    val equalsProjectId = equal("projectId", command.projectId)
    val equalsTaskStart = equal("taskTimeDetails.start", command.start)
    val andCondition = and(equalsProjectId, equalsTaskStart)

    deleteOne(andCondition, command)
  }

  private def deleteOne[C <: Command](andCondition: Bson, command: C)(implicit writes: Writes[C]) = {
    collection.findOneAndUpdate(andCondition, combine(set("taskTimeDetails.delete", LocalDateTime.now())))
      .toFuture()
      .map(_ => getSuccessResult[C](OK, "Task deleted", command))
      .recover { case _ => prepareErrorResult() }
  }

  def update(command: UpdateTaskCommand): Future[Result] = {
    val equalsProjectId = equal("projectId", command.projectIdOld)
    val equalsTaskStart = equal("taskTimeDetails.start", command.startDateOld)
    val andCondition = and(equalsProjectId, equalsTaskStart)

    for {
      deleteResult <- deleteOne(andCondition, command)
      createResult <- create(mapToCreateCommand(command))
    } yield mergeFutures(deleteResult, createResult, command)
  }

  def mergeFutures(deleteResult: Result, createResult: Result, command: UpdateTaskCommand): Result = {
    if (areStatusesSuccessful(deleteResult, createResult)){
      getSuccessResult[UpdateTaskCommand](OK, "Task updated", command)
    } else {
      prepareErrorResult()
    }
  }

  def deleteAll(command: DeleteProjectCommand): Future[Result] = {
    val equalsProjectId = Filters.eq("projectId", command.projectId)
    val deleteIsNull = Filters.eq("taskTimeDetails.delete", null)
    val andClause = Filters.and(equalsProjectId, deleteIsNull)

    collection.updateMany(andClause, combine(set("taskTimeDetails.delete", LocalDateTime.now())))
      .toFuture()
      .map(_ => getSuccessResult[DeleteProjectCommand](OK, "Tasks deleted", command))
      .recover { case _ => prepareErrorResult() }
  }

  private def areStatusesSuccessful(deleteResult: Result, createResult: Result) =
    deleteResult.header.status == OK && createResult.header.status == CREATED

  private def mapToCreateCommand(command: UpdateTaskCommand) =
    CreateTaskCommand(command.projectIdNew, command.authorIdNew, command.taskTimeDetails, command.volume, command.comment)

  private def prepareErrorResult(): Result = {
    val json = Json.toJson(Response[String](success = false, "Database error", EMPTY))
    getResult(ResponseHeader(INTERNAL_SERVER_ERROR), json)
  }
}

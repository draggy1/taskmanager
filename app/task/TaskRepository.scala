package task

import com.mongodb.client.model.Filters.{and, or}
import common.StringUtils.EMPTY
import common.responses.Response
import common.responses.Response.getResult
import configuration.MongoDbManager
import org.bson.UuidRepresentation
import org.bson.codecs.UuidCodec
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.configuration.{CodecProvider, CodecRegistries, CodecRegistry}
import org.bson.conversions.Bson
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.bson.codecs.Macros
import org.mongodb.scala.model.Filters.{equal, gt, lt}
import org.mongodb.scala.model.Updates.{combine, set}
import play.api.http.Status.{CREATED, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.{ResponseHeader, Result}
import task.commands.{CreateTaskCommand, DeleteTaskCommand, UpdateTaskCommand}
import task.queries.{GetTaskByProjectIdAndStartQuery, GetTaskByProjectIdAndTimeDetailsQuery, GetTaskByProjectIdQuery}

import java.time.LocalDateTime
import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class TaskRepository @Inject()(config: MongoDbManager){
  private val taskCodecProvider: CodecProvider = Macros.createCodecProvider[Task]()
  private val taskDurationCodecProvider: CodecProvider = Macros.createCodecProvider[TaskDuration]()
  private val timeDetailsCodecProvider: CodecProvider = Macros.createCodecProvider[TaskTimeDetails]()
  private val uuidCodec = new UuidCodec(UuidRepresentation.STANDARD)
  private val uuidRegistry: CodecRegistry = CodecRegistries.fromCodecs(uuidCodec)
  private val codecRegistry: CodecRegistry =
    fromRegistries(fromProviders(taskCodecProvider, timeDetailsCodecProvider, taskDurationCodecProvider), uuidRegistry, DEFAULT_CODEC_REGISTRY)

  private val collection: MongoCollection[Task] = config.database
    .withCodecRegistry(codecRegistry)
    .getCollection("task")

  implicit val taskDuration: Writes[TaskDuration] = (taskDuration: TaskDuration) => Json.obj(
    "hours" -> taskDuration.hoursValue,
    "minutes" -> taskDuration.minutesValue)

  private implicit val timeDetailsWrites: Writes[TaskTimeDetails] = (taskTimeDetails: TaskTimeDetails) => Json.obj(
    "start_date" -> taskTimeDetails.start,
    "duration" -> taskTimeDetails.duration)

  implicit val taskWrites: Writes[CreateTaskCommand] = (command: CreateTaskCommand) => Json.obj(
    "project_id" -> command.projectId,
    "author_id" -> command.authorId,
    "task_time_details" -> command.taskTimeDetails,
    "volume" -> command.volume,
    "comment" -> command.comment)

  implicit val taskDeleteWrites: Writes[DeleteTaskCommand] = (command: DeleteTaskCommand) => Json.obj(
    "project_id" -> command.projectId,
    "author_id" -> command.authorId,
    "start_date" -> command.start)

  implicit val taskUpdateWrites: Writes[UpdateTaskCommand] = (command: UpdateTaskCommand) => Json.obj(
    "project_id_old" -> command.projectIdOld,
    "project_id_new"-> command.projectIdNew,
    "author_id_old" -> command.authorIdOld,
    "author_id_new" -> command.authorIdNew,
    "start_date_old" -> command.startDateOld,
    "start_date_new" -> command.taskTimeDetails.start,
    "duration" -> command.taskTimeDetails.duration,
    "volume" -> command.volume,
    "comment" -> command.comment)

  def create(command: CreateTaskCommand): Future[Result] = {
    val task =
      Task(new ObjectId(), command.projectId, command.authorId, command.taskTimeDetails, command.volume, command.comment)

    val json = Json.toJson(Response[CreateTaskCommand](success = true, "Task created", command))

    collection.insertOne(task)
      .toFuture()
      .map(_ => prepareResult(CREATED, json))
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

  def delete(command: DeleteTaskCommand): Future[Result] = {
    val equalsProjectId = equal("projectId", command.projectId)
    val equalsTaskStart = equal("taskTimeDetails.start", command.start)
    val andCondition = and(equalsProjectId, equalsTaskStart)

    val json = Json.toJson(Response[DeleteTaskCommand](success = true, "Task deleted", command))

    delete(andCondition, json)
  }

  private def delete(andCondition: Bson, json: JsValue) = {
    collection.findOneAndUpdate(andCondition, combine(set("taskTimeDetails.delete", LocalDateTime.now())))
      .toFuture()
      .map(_ => prepareResult(OK, json))
      .recover { case _ => prepareErrorResult() }
  }

  def update(command: UpdateTaskCommand): Future[Result] = {
    val equalsProjectId = equal("projectId", command.projectIdOld)
    val equalsTaskStart = equal("taskTimeDetails.start", command.startDateOld)
    val andCondition = and(equalsProjectId, equalsTaskStart)

    val json = Json.toJson(Response[String](success = true, "Task updated", EMPTY))

    for {
      deleteResult <- delete(andCondition, json)
      createResult <- create(mapToCreateCommand(command))
    } yield mergeFutures(deleteResult, createResult, command)
  }

  def mergeFutures(deleteResult: Result, createResult: Result, command: UpdateTaskCommand): Result = {
    if (areStatusesSuccessful(deleteResult, createResult)){
      val json = Json.toJson(Response[UpdateTaskCommand](success = true, "Task updated", command))
      prepareResult(OK, json)
    } else {
      prepareErrorResult()
    }
  }

  private def areStatusesSuccessful(deleteResult: Result, createResult: Result) =
    deleteResult.header.status == OK && createResult.header.status == CREATED

  private def mapToCreateCommand(command: UpdateTaskCommand) =
    CreateTaskCommand(command.projectIdNew, command.authorIdNew, command.taskTimeDetails, command.volume, command.comment)

  private def prepareResult(status: Int, json: JsValue): Result =
    getResult(ResponseHeader(status), json)

  private def prepareErrorResult(): Result = {
    val json = Json.toJson(Response[String](success = false, "Database error", EMPTY))
    getResult(ResponseHeader(INTERNAL_SERVER_ERROR), json)
  }
}

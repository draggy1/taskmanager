package project

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.and
import common.StringUtils.EMPTY
import common.responses.Response
import common.responses.Response.getResult
import configuration.MongoDbManager
import io.jvm.uuid.UUID
import org.bson.UuidRepresentation
import org.bson.codecs.UuidCodec
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.configuration.{CodecProvider, CodecRegistries, CodecRegistry}
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.codecs.Macros
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Updates.{combine, set}
import play.api.http.Status.{CREATED, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.{ResponseHeader, Result}
import project.commands.{CreateProjectCommand, DeleteProjectCommand, UpdateProjectCommand}

import java.time.LocalDateTime
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
case class ProjectRepository @Inject()(config: MongoDbManager) {
  private val projectCodecProvider: CodecProvider = Macros.createCodecProvider[Project]()

  private val uuidCodec = new UuidCodec(UuidRepresentation.STANDARD)
  private val uuidRegistry: CodecRegistry = CodecRegistries.fromCodecs(uuidCodec)
  private val codecRegistry: CodecRegistry = fromRegistries(fromProviders(projectCodecProvider), uuidRegistry, DEFAULT_CODEC_REGISTRY)

  private val collection: MongoCollection[Project] = config.database
    .withCodecRegistry(codecRegistry)
    .getCollection("project")

  implicit val projectWrites: Writes[CreateProjectCommand] = (data: CreateProjectCommand) => Json.obj(
    "author_id" -> data.authorId,
    "project_id" -> data.projectId)

  implicit val updateResponseWrites: Writes[UpdateProjectCommand] = (data: UpdateProjectCommand) => Json.obj(
    "author_id" -> data.authorId,
    "project_id_old" -> data.projectIdOld,
    "project_id_new" -> data.projectIdNew)

  implicit val deleteResponseWrites: Writes[DeleteProjectCommand] = (data: DeleteProjectCommand) => Json.obj(
    "author_id" -> data.authorId,
    "project_id" -> data.projectId)

  implicit val successWrites: Writes[Response[CreateProjectCommand]] = Json.writes[Response[CreateProjectCommand]]

  def create(command: CreateProjectCommand): Future[Result] = {
    val project = Project(command.authorId, command.projectId)

    collection.insertOne(project)
      .toFuture()
      .map(_ => prepareSuccessResult(command))
      .recover { case _ => prepareErrorResult() }
  }

  def find(projectId: String): Future[Option[Project]] =
    collection.find(equal("projectId", projectId))
      .first()
      .toFutureOption()

  def find(projectId: String, authorId: UUID): Future[Option[Project]] = {
    val equalProjectId = equal("projectId", projectId)
    val equalAuthorId = equal("authorId", authorId)

    collection.find(and(equalProjectId, equalAuthorId))
      .first()
      .toFutureOption()
  }

  def update(command: UpdateProjectCommand): Future[Result] = {
    val equalsProjectId = Filters.eq("projectId", command.projectIdOld)
    val setter = combine(set("projectId", command.projectIdNew))
    val json = Json.toJson(Response[UpdateProjectCommand](success = true, "Project updated", command))

    performUpdateByProvidedData(json, OK, equalsProjectId, setter)
  }

  def delete(command: DeleteProjectCommand): Future[Result] = {
    val equalsProjectId = Filters.eq("projectId", command.projectId)
    val setter = combine(set("deleted", LocalDateTime.now()))
    val json = Json.toJson(Response[DeleteProjectCommand](success = true, "Project deleted", command))

    performUpdateByProvidedData(json, OK, equalsProjectId, setter)
  }

  private def performUpdateByProvidedData(json: JsValue, status: Int, equalsProjectId: Bson, setter: Bson) =
    collection.findOneAndUpdate(equalsProjectId, setter)
      .toFuture()
      .map(_ => prepareResult(json, status))
      .recover {
        case _ =>
          val json = Json.toJson(Response[String](success = false, "Database error", EMPTY))
          prepareResult(json, INTERNAL_SERVER_ERROR)
      }

  private def prepareResult(json: JsValue, status: Int): Result = {
    getResult(ResponseHeader(status), json)
  }

  private def prepareSuccessResult(command: CreateProjectCommand): Result = {
    val json = Json.toJson(Response[CreateProjectCommand](success = true, "Project created", command))
    getResult(ResponseHeader(CREATED), json)
  }

  private def prepareErrorResult(): Result = {
    val json = Json.toJson(Response[String](success = false, "Database error", EMPTY))
    getResult(ResponseHeader(INTERNAL_SERVER_ERROR), json)
  }
}

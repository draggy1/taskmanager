package project

import com.mongodb.client.model.Filters
import common.StringUtils.EMPTY
import common.responses.Response.getResult
import common.responses.{ProjectUpdatedResponse, Response}
import configuration.MongoDbManager
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.configuration.{CodecProvider, CodecRegistry}
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.codecs.Macros
import org.mongodb.scala.bson.{BsonDocument, BsonString, ObjectId}
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Updates.{combine, set}
import play.api.http.Status.{CREATED, INTERNAL_SERVER_ERROR}
import play.api.libs.json.{Json, Writes}
import play.api.mvc.{ResponseHeader, Result}
import project.commands.{CreateProjectCommand, UpdateProjectCommand}

import java.time.LocalDateTime
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
case class ProjectRepository @Inject()(config: MongoDbManager) {
  private val projectCodecProvider: CodecProvider = Macros.createCodecProvider[Project]()

  private val codecRegistry: CodecRegistry = fromRegistries(fromProviders(projectCodecProvider), DEFAULT_CODEC_REGISTRY)

  private val collection: MongoCollection[Project] = config.database
    .withCodecRegistry(codecRegistry)
    .getCollection("project")

  implicit val projectWrites: Writes[Project] = (data: Project) => Json.obj(
    "user_id" -> data.userId,
    "project_id" -> data.projectId)

  implicit val updateResponseWrites: Writes[ProjectUpdatedResponse] = (data: ProjectUpdatedResponse) => Json.obj(
    "project_id_old" -> data.oldProjectId,
    "project_id_new" -> data.newProjectId)

  implicit val successWrites: Writes[Response[Project]] = Json.writes[Response[Project]]

  def create(command: CreateProjectCommand): Future[Result] = {
    val project = Project(new ObjectId(), command.userId, command.projectId, LocalDateTime.now())

    collection.insertOne(project)
      .toFuture()
      .map(_ => prepareSuccessResult(project))
      .recover { case _ => prepareErrorResult() }
  }


  def find(projectId: String): Future[Option[Project]] =
    collection.find(equal("projectId", projectId))
      .first()
      .toFutureOption()

  def updateProjectId(command: UpdateProjectCommand): Future[Result] = {
    val newProjectId = new BsonDocument("projectId", BsonString(command.projectIdNew))

    collection.findOneAndUpdate(Filters.eq("projectId", command.projectIdOld), combine(set("projectId", command.projectIdNew)))
      .toFuture()
      .map(_ => prepareSuccessUpdateResult(command))
      //.recover { case _ => prepareErrorResult() }
  }

  private def prepareSuccessResult(project: Project): Result = {
    val json = Json.toJson(Response[Project](success = true, "Project created", project))
    getResult(ResponseHeader(CREATED), json)
  }

  def prepareSuccessUpdateResult(command: UpdateProjectCommand): Result = {
    val json = Json.toJson(Response[ProjectUpdatedResponse](success = true, "Project updated", ProjectUpdatedResponse(command.projectIdOld, command.projectIdNew)))
    getResult(ResponseHeader(CREATED), json)
  }

  private def prepareErrorResult(): Result = {
    val json = Json.toJson(Response[String](success = false, "Database error", EMPTY))
    getResult(ResponseHeader(INTERNAL_SERVER_ERROR), json)
  }
}

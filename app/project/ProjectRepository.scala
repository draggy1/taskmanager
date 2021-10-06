package project

import common.Response
import common.Response.getResult
import common.StringUtils.EMPTY
import configuration.MongoDbManager
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.configuration.{CodecProvider, CodecRegistry}
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.codecs.Macros
import org.mongodb.scala.model.Filters.equal
import play.api.http.Status.{CREATED, INTERNAL_SERVER_ERROR}
import play.api.libs.json.{Json, Writes}
import play.api.mvc.{ResponseHeader, Result}

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

  implicit val successWrites: Writes[Response[Project]] = Json.writes[Response[Project]]

  def create(project: Project): Future[Result] = {
    collection.insertOne(project)
      .toFuture()
      .map(_ => prepareSuccessResult(project))
      .recover { case _ => prepareErrorResult() }
  }

  private def prepareSuccessResult(project: Project): Result = {
    val json = Json.toJson(Response[Project](success = true, "Project created", project))
    getResult(ResponseHeader(CREATED), json)
  }

  private def prepareErrorResult(): Result = {
    val json = Json.toJson(Response[String](success = false, "Database error", EMPTY))
    getResult(ResponseHeader(INTERNAL_SERVER_ERROR), json)
  }

  def find(projectId: String): Future[Option[Project]] =
    collection.find(equal("projectId", projectId))
      .first()
      .toFutureOption()
}

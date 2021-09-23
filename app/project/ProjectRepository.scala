package project

import configuration.MongoDbManager
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.configuration.{CodecProvider, CodecRegistry}
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.codecs.Macros
import org.mongodb.scala.model.Filters.equal
import play.api.mvc.Result
import play.api.mvc.Results.{Created, InternalServerError}

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

  def create(project: Project): Future[Result] =
    collection.insertOne(project)
      .toFuture()
      .map(_ => Created("Project created"))
      .recover({ case _ => InternalServerError("Database error") })

  def find(projectId: String): Future[Option[Project]] =
    collection.find(equal("projectId", projectId))
      .first()
      .toFutureOption()
}

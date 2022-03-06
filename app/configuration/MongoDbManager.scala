package configuration

import org.bson.UuidRepresentation
import org.bson.codecs.UuidCodec
import org.bson.codecs.configuration.{CodecProvider, CodecRegistries, CodecRegistry}
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros
import org.mongodb.scala.{MongoClient, MongoDatabase}
import task.{Task, TaskDuration, TaskTimeDetails}

import javax.inject.{Inject, Singleton}

@Singleton
case class MongoDbManager @Inject()(){
  private val taskCodecProvider: CodecProvider = Macros.createCodecProvider[Task]()
  private val taskDurationCodecProvider: CodecProvider = Macros.createCodecProvider[TaskDuration]()
  private val timeDetailsCodecProvider: CodecProvider = Macros.createCodecProvider[TaskTimeDetails]()
  private val uuidCodec = new UuidCodec(UuidRepresentation.STANDARD)
  private val uuidRegistry: CodecRegistry = CodecRegistries.fromCodecs(uuidCodec)

  val mongoClient: MongoClient = MongoClient()
  val database: MongoDatabase = mongoClient.getDatabase("taskmanager_db")
  val taskCodecRegistry: CodecRegistry =
    fromRegistries(fromProviders(taskCodecProvider, timeDetailsCodecProvider, taskDurationCodecProvider), uuidRegistry, DEFAULT_CODEC_REGISTRY)
}

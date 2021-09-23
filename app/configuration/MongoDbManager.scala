package configuration

import org.mongodb.scala.{MongoClient, MongoDatabase}

import javax.inject.{Inject, Singleton}

@Singleton
case class MongoDbManager @Inject()(){
  val mongoClient: MongoClient = MongoClient()
  val database: MongoDatabase = mongoClient.getDatabase("taskmanager_db")
}

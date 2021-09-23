package it

import configuration.MongoDbManager
import org.mongodb.scala.{MongoClient, MongoDatabase}

import javax.inject.Inject

case class MongoDbManagerTest @Inject()() extends MongoDbManager{
  override val mongoClient: MongoClient = MongoClient("mongodb://localhost:28888")
  override val database: MongoDatabase = mongoClient.getDatabase("test_taskmanager_db")
}

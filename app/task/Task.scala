package task

import org.mongodb.scala.bson.ObjectId

case class Task(_id: ObjectId, projectId: String, taskTimeDetails: TaskTimeDetails, volume: Option[Int],
                comment: Option[String])

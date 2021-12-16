package task

import io.jvm.uuid.UUID
import org.mongodb.scala.bson.ObjectId

case class Task(_id: ObjectId, projectId: String, authorId: UUID, taskTimeDetails: TaskTimeDetails, volume: Option[Int],
                comment: Option[String])

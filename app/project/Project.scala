package project

import io.jvm.uuid.UUID
import org.mongodb.scala.bson.ObjectId

import java.time.LocalDateTime

case object Project {
  def apply(userId: UUID, projectId: String, timestamp: LocalDateTime): Project =
    Project(new ObjectId(), userId, projectId, timestamp)
}
case class Project(_id: ObjectId, userId: UUID, projectId: String, timestamp: LocalDateTime)





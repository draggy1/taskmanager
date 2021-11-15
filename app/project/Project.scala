package project

import io.jvm.uuid.UUID
import org.mongodb.scala.bson.ObjectId

import java.time.LocalDateTime

case object Project {
  def apply(authorId: UUID, projectId: String): Project =
    Project(new ObjectId(), authorId, projectId, LocalDateTime.now())
}
case class Project(_id: ObjectId, authorId: UUID, projectId: String, timestamp: LocalDateTime)





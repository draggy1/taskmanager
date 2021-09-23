package project

import io.jvm.uuid.UUID
import org.mongodb.scala.bson.ObjectId

object Project {
  def apply(userId: UUID, projectId: String): Project =
    Project(new ObjectId(), userId, projectId)
}
case class Project(_id: ObjectId, userId: UUID, projectId: String)



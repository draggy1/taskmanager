package task

import io.jvm.uuid.UUID
import org.mongodb.scala.bson.ObjectId

case object Task {
  def apply(projectId: String, authorId: UUID, taskTimeDetails: TaskTimeDetails): Task =
    Task(new ObjectId(), projectId, authorId, taskTimeDetails, Option.empty, Option.empty)
}

case class Task(_id: ObjectId, projectId: String, authorId: UUID, taskTimeDetails: TaskTimeDetails, volume: Option[Int],
                comment: Option[String])

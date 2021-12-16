package task.queries

import java.time.LocalDateTime

case class GetTaskByProjectIdAndStartQuery(projectId: String, start:LocalDateTime)

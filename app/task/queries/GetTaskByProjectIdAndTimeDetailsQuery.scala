package task.queries

import task.TaskTimeDetails

case class GetTaskByProjectIdAndTimeDetailsQuery(projectId: String, taskTimeDetails: TaskTimeDetails)

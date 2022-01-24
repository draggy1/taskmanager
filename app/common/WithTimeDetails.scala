package common

import task.{TaskDuration, TaskTimeDetails}

trait WithTimeDetails {
  def getProjectId: String
  def getTimeDetails: TaskTimeDetails
  def getDuration: TaskDuration
}

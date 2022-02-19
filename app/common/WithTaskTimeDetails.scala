package common

import task.TaskTimeDetails

trait WithTaskTimeDetails {
  def getTimeDetails: TaskTimeDetails
}

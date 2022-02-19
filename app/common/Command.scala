package common

import io.jvm.uuid.UUID

trait Command {
  def getAuthorId: UUID
  def getProjectId: String
  def checkIfProjectIdIsBlank(): Boolean
  def getProjectToCheckIfExist: String
}

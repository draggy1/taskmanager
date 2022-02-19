package common

import io.jvm.uuid.UUID

trait Command {
  def getAuthorId: UUID
  def getProjectId: String
  def isProjectIdBlank: Boolean
  def isAuthorIdBlank: Boolean
  def getProjectToCheckIfExist: String
}

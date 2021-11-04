package project.queries

import io.jvm.uuid.UUID

case class GetProjectByIdAndAuthorIdQuery(projectId: String, authorId: UUID)
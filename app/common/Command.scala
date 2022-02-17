package common

import io.jvm.uuid.UUID

abstract class Command(val projectId: String, val authorId: UUID)

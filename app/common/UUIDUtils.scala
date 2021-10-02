package common

import io.jvm.uuid.UUID

import scala.util.matching.Regex

case object UUIDUtils{
  val uuidRegex: Regex = "^[0-9a-f]{8}-[0-9a-f]{4}-[5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$".r
  val UUID_NIL: UUID = UUID("00000000-0000-0000-0000-000000000000")
}

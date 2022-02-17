package common

import project.Project
import task.Task

import scala.concurrent.Future

case class ValidationContext[C <: Command](command: C, project: Future[Option[Project]], task: Future[Option[Task]])

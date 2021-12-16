package controllers

import authentication.AuthenticationHandler
import common.responses.Response.mapErrorToResult
import controllers.steps.task.{CreateTaskSteps, DeleteTaskSteps}
import play.api.mvc._
import project.ProjectAggregate
import task.TaskAggregate

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class TaskController @Inject()(val controllerComponents: ControllerComponents,
                               taskAggregate: TaskAggregate,
                               projectAggregate: ProjectAggregate,
                               authHandler: AuthenticationHandler) extends BaseController {

  def create(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    CreateTaskSteps(taskAggregate, projectAggregate, authHandler)
      .prepare()
      .apply(request)
      .flatMap {
        case Left(error) => Future.successful(mapErrorToResult(error))
        case Right(result) => result
      }
  }

  def delete(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    DeleteTaskSteps(taskAggregate, projectAggregate, authHandler)
      .prepare()
      .apply(request)
      .flatMap {
        case Left(error) => Future.successful(mapErrorToResult(error))
        case Right(result) => result
      }
  }
}

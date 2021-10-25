package controllers

import authentication.AuthenticationHandler
import common.responses.Response.mapErrorToResult
import controllers.actions.{CreateProjectActions, UpdateProjectActions}
import play.api.mvc._
import project.ProjectAggregate

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ProjectController @Inject()(val controllerComponents: ControllerComponents,
                                  aggregate: ProjectAggregate,
                                  authHandler: AuthenticationHandler) extends BaseController {
  /**
   * Create an endpoint for setting up a new project
   *
   * @return
   */
  def createProject(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] => {
    CreateProjectActions(aggregate, authHandler)
      .prepare()
      .apply(request)
      .flatMap {
        case Left(error) => Future.successful(mapErrorToResult(error))
        case Right(result) => result
      }
    }
  }

  /**
   * Create an endpoint for update the project id
   *
   * @return
   */
  def updateProjectId(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    UpdateProjectActions(aggregate, authHandler)
      .prepare()
      .apply(request)
      .flatMap {
        case Left(error) => Future.successful(mapErrorToResult(error))
        case Right(result) => result
      }
  }
}


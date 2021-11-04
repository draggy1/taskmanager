package controllers

import authentication.AuthenticationHandler
import io.jvm.uuid.UUID
import org.mockito.Mockito.when
import org.scalatest.GivenWhenThen
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.http.Status.{BAD_REQUEST, CREATED}
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.Created
import play.api.test.Helpers.{AUTHORIZATION, POST, contentAsJson, defaultAwaitTimeout, status}
import play.api.test.{FakeRequest, Helpers}
import project.commands.CreateProjectCommand
import project.ProjectAggregate
import project.queries.GetProjectByIdQuery
import project.validators.CreateProjectValidator

import scala.concurrent.Future


class ProjectControllerTest extends PlaySpec with MockitoSugar with GivenWhenThen with ScalaFutures{
  private implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))
  "ProjectController#createProject" should {
    "be valid" in {
      Given("Project aggregate, authentication handler, validator and request")
      val createCommand = CreateProjectCommand(UUID("e54e5692-60d3-4c84-a251-66aa998d7cb1"), "unique_project_id_1")
      val expectedJson = """
      {
        "success": true,
        "message": "Project created",
        "data": {
          "author_id": "e54e5692-60d3-4c84-a251-66aa998d7cb1",
          "project_id": "unique_project_id_1"
        }
      }
      """
      val projectAggregate = mock[ProjectAggregate]
      when(projectAggregate.createProject(createCommand)).thenReturn(Future.successful(Created(expectedJson)))
      when(projectAggregate.getProject(GetProjectByIdQuery("unique_project_id_1")))
        .thenReturn(Future.successful(Option.empty))

      val validator = mock[CreateProjectValidator]
      when(validator.validate(createCommand)).thenReturn(Future.successful(Right(createCommand)))

      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val authHandler = AuthenticationHandler(config)

      val givenRequest = FakeRequest()
        .withMethod(POST)
        .withHeaders((AUTHORIZATION, "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdXRob3JfaWQiOiJlNTRlNTY5Mi02MGQzLTRjODQtYTI1MS02NmFhOTk4ZDdjYjEiLCJwcm9qZWN0X2lkIjoidW5pcXVlX3Byb2plY3RfaWRfMSJ9.zoHqtGCKmUekm_96wNXHY2VexdxeYOnUOuoc0H22Z34"))

      When("ProjectController#createProject method is performed")
      val controller = new ProjectController(Helpers.stubControllerComponents(), projectAggregate, authHandler)
      val result: Future[Result] = controller.createProject().apply(givenRequest)

      Then("Result should be Created with expected json as body")
      status(result) mustBe CREATED
      contentAsJson(result) mustBe Json.parse(expectedJson)
    }
  }
  "ProjectController#createProject" should {
    "be failed because of authorization" in {
      Given("Project aggregate, authentication handler, validator, request, command and config")
      val projectAggregate = mock[ProjectAggregate]

      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(POST)

      When("ProjectController#createProject method is performed")
      val controller = new ProjectController(Helpers.stubControllerComponents(), projectAggregate, authHandler)
      val result: Future[Result] = controller.createProject().apply(givenRequest)

      Then("Result should be Created with message: \"Project created\"")
      status(result) mustBe BAD_REQUEST
      val expectedJson = """
      {
        "success" : false,
        "message" : "Request not contains authorization header",
        "data" : ""
      }
      """
      contentAsJson(result) mustBe Json.parse(expectedJson)
    }
  }
}

package controllers

import authentication.AuthenticationHandler
import io.jvm.uuid.UUID
import org.mockito.Mockito.when
import org.scalatest.GivenWhenThen
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.http.Status.{BAD_REQUEST, CREATED}
import play.api.mvc.Result
import play.api.mvc.Results.Created
import play.api.test.Helpers.{AUTHORIZATION, POST, contentAsString, defaultAwaitTimeout, status}
import play.api.test.{FakeRequest, Helpers}
import project.{ProjectAggregate, ProjectValidator}
import project.commands.CreateProjectCommand

import scala.concurrent.Future


class ProjectControllerTest extends PlaySpec with MockitoSugar with GivenWhenThen{
  "ProjectController#createProject" should {
    "be valid" in {
      Given("Project aggregate, authentication handler, validator and request")
      val projectAggregate = mock[ProjectAggregate]

      val createCommand = CreateProjectCommand(UUID("e54e5692-60d3-4c84-a251-66aa998d7cb1"), "unique_project_id_1")
      when(projectAggregate.createProject(createCommand)).thenReturn(Future.successful(Created("Project created")))

      val validator = mock[ProjectValidator]
      when(validator.validate(createCommand)).thenReturn(Future.successful(Right(createCommand)))

      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(POST)
        .withHeaders((AUTHORIZATION, "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiZTU0ZTU2OTItNjBkMy00Yzg0LWEyNTEtNjZhYTk5OGQ3Y2IxIiwicHJvamVjdF9pZCI6InVuaXF1ZV9wcm9qZWN0X2lkXzEifQ.PNDMAcOVUQXLaVR1Tp2wyAQhNUOBi7Luq5MOrlINJTg"))

      When("ProjectController#createProject method is performed")
      val controller = new ProjectController(Helpers.stubControllerComponents(), projectAggregate, authHandler, validator)
      val result: Future[Result] = controller.createProject().apply(givenRequest)

      Then("Result should be Created with message: \"Project created\"")
      status(result) mustBe CREATED
      contentAsString(result) mustBe "Project created"
    }
  }

  "ProjectController#createProject" should {
    "be failed because of authorization" in {
      Given("Project aggregate, authentication handler, validator, request, command and config")
      val projectAggregate = mock[ProjectAggregate]

      val validator = mock[ProjectValidator]

      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(POST)

      When("ProjectController#createProject method is performed")
      val controller = new ProjectController(Helpers.stubControllerComponents(), projectAggregate, authHandler, validator)
      val result: Future[Result] = controller.createProject().apply(givenRequest)

      Then("Result should be Created with message: \"Project created\"")
      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "Request not contains authorization header"
    }
  }
}

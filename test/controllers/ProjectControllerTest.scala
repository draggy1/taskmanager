package controllers

import akka.util.ByteString
import authentication.AuthenticationHandler
import common.StringUtils
import io.jvm.uuid.UUID
import org.mockito.Mockito.when
import org.scalatest.GivenWhenThen
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.http.Status.{BAD_REQUEST, CREATED, OK}
import play.api.http.{ContentTypes, HttpEntity}
import play.api.libs.json.Json
import play.api.mvc.{ResponseHeader, Result}
import play.api.test.Helpers.{AUTHORIZATION, POST, contentAsJson, defaultAwaitTimeout, status}
import play.api.test.{FakeRequest, Helpers}
import project.commands.{CreateProjectCommand, UpdateProjectCommand}
import project.{Project, ProjectAggregate, ProjectRepository}

import scala.concurrent.Future

class ProjectControllerTest extends PlaySpec with MockitoSugar with GivenWhenThen with ScalaFutures {
  private implicit val defaultPatience: PatienceConfig = {
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))
  }

  "ProjectController#create" should {
    "be failed because of empty Bearer token" in {
      Given("Data needed to prepare request, expected result")
      val bearer = StringUtils.EMPTY

      val expectedJson =
        """
      {
        "success":false,
        "message":"Request not contains bearer token",
        "data": ""
      }
      """

      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val repository = mock[ProjectRepository]

      val projectAggregate = new ProjectAggregate(repository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(POST)
        .withHeaders((AUTHORIZATION, bearer))

      When("Creation method is performed")
      val controller = new ProjectController(Helpers.stubControllerComponents(), projectAggregate, authHandler)
      val result: Future[Result] = controller.create().apply(givenRequest)

      Then("Result should be with status Created with expected json as body")
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.parse(expectedJson)
    }

    "be successful" in {
      Given("Data needed to prepare request, expected result")
      val authorId = UUID("e54e5692-60d3-4c84-a251-66aa998d7cb1")
      val projectId = "unique_project_id_1"

      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" +
        ".eyJhdXRob3JfaWQiOiJlNTRlNTY5Mi02MGQzLTRjODQtYTI1MS02NmFhOTk4ZDdjYjEiLCJwcm9qZWN0X2lkIjoidW5pcXVlX3Byb2plY3RfaWRfMSJ9" +
        ".zoHqtGCKmUekm_96wNXHY2VexdxeYOnUOuoc0H22Z34"

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

      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val repository = mock[ProjectRepository]
      val body = HttpEntity.Strict(ByteString(expectedJson), Some(ContentTypes.JSON))
      val response = Result(ResponseHeader(CREATED), body)

      when(repository.find(projectId)).thenReturn(Future.successful(Option.empty))
      when(repository.create(CreateProjectCommand(authorId, projectId))).thenReturn(Future.successful(response))

      val projectAggregate = new ProjectAggregate(repository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(POST)
        .withHeaders((AUTHORIZATION, bearer))

      When("Creation method is performed")
      val controller = new ProjectController(Helpers.stubControllerComponents(), projectAggregate, authHandler)
      val result: Future[Result] = controller.create().apply(givenRequest)

      Then("Result should be with status Created with expected json as body")
      status(result) mustBe CREATED
      contentAsJson(result) mustBe Json.parse(expectedJson)
    }

    "be failed because of duplication" in {
      Given("Data needed to prepare request, expected result")
      val authorId = UUID("e54e5692-60d3-4c84-a251-66aa998d7cb1")
      val projectId = "unique_project_id_1"

      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" +
        ".eyJhdXRob3JfaWQiOiJlNTRlNTY5Mi02MGQzLTRjODQtYTI1MS02NmFhOTk4ZDdjYjEiLCJwcm9qZWN0X2lkIjoidW5pcXVlX3Byb2plY3RfaWRfMSJ9" +
        ".zoHqtGCKmUekm_96wNXHY2VexdxeYOnUOuoc0H22Z34"

      val expectedJson = """
      {
        "success": false,
        "message": "Provided project id already exist",
        "data": ""
      }
      """

      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val repository = mock[ProjectRepository]
      when(repository.find(projectId)).thenReturn(Future.successful(Some(Project(authorId, projectId))))

      val projectAggregate = new ProjectAggregate(repository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(POST)
        .withHeaders((AUTHORIZATION, bearer))

      When("Creation method is performed")
      val controller = new ProjectController(Helpers.stubControllerComponents(), projectAggregate, authHandler)
      val result: Future[Result] = controller.create().apply(givenRequest)

      Then("Result should be with status Bad Request with expected json as body")
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.parse(expectedJson)
    }

    "be failed because of empty project id" in {
      Given("Data needed to prepare request, expected result")

      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" +
        ".eyJhdXRob3JfaWQiOiJlNTRlNTY5Mi02MGQzLTRjODQtYTI1MS02NmFhOTk4ZDdjYjEifQ" +
        ".D4H9Tj0FjaGMNs_MSfomXqIsG4N3PIuNhs9zCvl23AM"

      val expectedJson = """
      {
        "success": false,
        "message": "Provided project id is empty",
        "data": ""
      }
      """

      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val repository = mock[ProjectRepository]

      val projectAggregate = new ProjectAggregate(repository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(POST)
        .withHeaders((AUTHORIZATION, bearer))

      When("Creation method is performed")
      val controller = new ProjectController(Helpers.stubControllerComponents(), projectAggregate, authHandler)
      val result: Future[Result] = controller.create().apply(givenRequest)

      Then("Result should be with status Bad Request with expected json as body")
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.parse(expectedJson)
    }

    "be failed because of not valid author id" in {
      Given("Data needed to prepare request, expected result")

      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" +
        ".eyJwcm9qZWN0X2lkIjoic29tZV9wcm9qZWN0X2lkIn0" +
        ".3II22dsqdSqhvKYc8ec3oEEcP3NX-Bsj_qHhCj_N9Ek"

      val expectedJson = """
      {
        "success": false,
        "message": "Provided author id is not valid",
        "data": ""
      }
      """

      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val repository = mock[ProjectRepository]

      val projectAggregate = new ProjectAggregate(repository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(POST)
        .withHeaders((AUTHORIZATION, bearer))

      When("Creation method is performed")
      val controller = new ProjectController(Helpers.stubControllerComponents(), projectAggregate, authHandler)
      val result: Future[Result] = controller.create().apply(givenRequest)

      Then("Result should be with status Bad Request with expected json as body")
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.parse(expectedJson)
    }
  }

  "ProjectController#update" should {
    "be failed because of empty Bearer token" in {
      val bearer = StringUtils.EMPTY
      val expectedJson =
        """
        {
          "success":false,
          "message":"Request not contains bearer token",
          "data": ""
        }
        """

        val config = mock[Configuration]
        when(config.get[String]("secret.key")).thenReturn("Test secret key")

        val repository = mock[ProjectRepository]

        val projectAggregate = new ProjectAggregate(repository)
        val authHandler = AuthenticationHandler(config)
        val givenRequest = FakeRequest()
          .withMethod(POST)
          .withHeaders((AUTHORIZATION, bearer))

        When("Update method is performed")
        val controller = new ProjectController(Helpers.stubControllerComponents(), projectAggregate, authHandler)
        val result: Future[Result] = controller.update().apply(givenRequest)

        Then("Result should be with status Ok with expected json as body")
        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.parse(expectedJson)
    }

    "be successful" in {
      Given("Data needed to prepare request, expected result")
      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" +
        ".eyJhdXRob3JfaWQiOiJlNTRlNTY5Mi02MGQzLTRjODQtYTI1MS02NmFhOTk4ZDdjYjEiLCJwcm9qZWN0X2lkX29sZCI6InVuaXF1ZV9wcm9qZWN0X2lkXzEiLCJwcm9qZWN0X2lkX25ldyI6Im5ld191bmlxdWVfcHJvamVjdF9pZF8xIn0" +
        ".Jf5ELYPYZvQyPYr8AdmjvRPpfcOdSFRW8jMJEm7Vfss"

      val expectedJson = """
      {
        "success": true,
        "message": "Project updated",
        "data": {
          "author_id": "e54e5692-60d3-4c84-a251-66aa998d7cb1",
          "project_id_old": "unique_project_id_1",
          "project_id_new": "new_unique_project_id_1"
        }
      }
      """

      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val authorId = UUID("e54e5692-60d3-4c84-a251-66aa998d7cb1")
      val projectIdOld = "unique_project_id_1"
      val projectIdNew = "new_unique_project_id_1"

      val body = HttpEntity.Strict(ByteString(expectedJson), Some(ContentTypes.JSON))
      val response = Result(ResponseHeader(OK), body)

      val repository = mock[ProjectRepository]
      when(repository.find(projectIdNew)).thenReturn(Future.successful(Option.empty))
      when(repository.find(projectIdOld)).thenReturn(Future.successful(Some(Project(authorId, projectIdOld))))
      when(repository.find(projectIdOld, authorId)).thenReturn(Future.successful(Some(Project(authorId, projectIdOld))))
      when(repository.update(UpdateProjectCommand(authorId, projectIdOld, projectIdNew)))
        .thenReturn(Future.successful(response))

      val projectAggregate = new ProjectAggregate(repository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(POST)
        .withHeaders((AUTHORIZATION, bearer))

      When("Update method is performed")
      val controller = new ProjectController(Helpers.stubControllerComponents(), projectAggregate, authHandler)
      val result: Future[Result] = controller.update().apply(givenRequest)

      Then("Result should be with status Ok with expected json as body")
      status(result) mustBe OK
      contentAsJson(result) mustBe Json.parse(expectedJson)
    }

    "be failed because of empty project id" in {
      Given("Data needed to prepare request, expected result")
      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" +
        ".eyJhdXRob3JfaWQiOiJlNTRlNTY5Mi02MGQzLTRjODQtYTI1MS02NmFhOTk4ZDdjYjEiLCJwcm9qZWN0X2lkX25ldyI6Im5ld191bmlxdWVfcHJvamVjdF9pZF8xIn0" +
        ".nrrewnJ0chLZDrtLOOL817a8WnEP1A7gasPfzE8_XMc"

      val expectedJson = """
      {
        "success": false,
        "message": "Provided project id is empty",
        "data": ""
      }
      """

      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val repository = mock[ProjectRepository]

      val projectAggregate = new ProjectAggregate(repository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(POST)
        .withHeaders((AUTHORIZATION, bearer))

      When("Update method is performed")
      val controller = new ProjectController(Helpers.stubControllerComponents(), projectAggregate, authHandler)
      val result: Future[Result] = controller.update().apply(givenRequest)

      Then("Result should be with status Bad request with expected json as body")
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.parse(expectedJson)
    }

    "be failed because of user is not the author of project" in {
      Given("Data needed to prepare request, expected result")
      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" +
        ".eyJhdXRob3JfaWQiOiJlNTRlNTY5Mi02MGQzLTRjODQtYTI1MS02NmFhOTk4ZDdjYjEiLCJwcm9qZWN0X2lkX29sZCI6InVuaXF1ZV9wcm9qZWN0X2lkXzEiLCJwcm9qZWN0X2lkX25ldyI6Im5ld191bmlxdWVfcHJvamVjdF9pZF8xIn0" +
        ".Jf5ELYPYZvQyPYr8AdmjvRPpfcOdSFRW8jMJEm7Vfss"

      val expectedJson = """
      {
        "success": false,
        "message": "Provided author is not author of the project",
        "data": ""
      }
      """

      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val authorId = UUID("e54e5692-60d3-4c84-a251-66aa998d7cb1")
      val projectIdOld = "unique_project_id_1"
      val projectIdNew = "new_unique_project_id_1"

      val repository = mock[ProjectRepository]
      when(repository.find(projectIdNew)).thenReturn(Future.successful(Option.empty))
      when(repository.find(projectIdOld)).thenReturn(Future.successful(Some(Project(authorId, projectIdOld))))
      when(repository.find(projectIdOld, authorId)).thenReturn(Future.successful(Option.empty))

      val projectAggregate = new ProjectAggregate(repository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(POST)
        .withHeaders((AUTHORIZATION, bearer))

      When("Update method is performed")
      val controller = new ProjectController(Helpers.stubControllerComponents(), projectAggregate, authHandler)
      val result: Future[Result] = controller.update().apply(givenRequest)

      Then("Result should be with status Bad request with expected json as body")
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.parse(expectedJson)
    }

    "be failed because of duplicated project" in {
      Given("Data needed to prepare request, expected result")
      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" +
        ".eyJhdXRob3JfaWQiOiJlNTRlNTY5Mi02MGQzLTRjODQtYTI1MS02NmFhOTk4ZDdjYjEiLCJwcm9qZWN0X2lkX29sZCI6InVuaXF1ZV9wcm9qZWN0X2lkXzEiLCJwcm9qZWN0X2lkX25ldyI6Im5ld191bmlxdWVfcHJvamVjdF9pZF8xIn0" +
        ".Jf5ELYPYZvQyPYr8AdmjvRPpfcOdSFRW8jMJEm7Vfss"

      val expectedJson = """
      {
        "success": false,
        "message": "Provided project id already exist",
        "data": ""
      }
      """

      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val authorId = UUID("e54e5692-60d3-4c84-a251-66aa998d7cb1")
      val projectIdNew = "new_unique_project_id_1"

      val repository = mock[ProjectRepository]
      when(repository.find(projectIdNew)).thenReturn(Future.successful(Some(Project(authorId, projectIdNew))))

      val projectAggregate = new ProjectAggregate(repository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(POST)
        .withHeaders((AUTHORIZATION, bearer))

      When("Update method is performed")
      val controller = new ProjectController(Helpers.stubControllerComponents(), projectAggregate, authHandler)
      val result: Future[Result] = controller.update().apply(givenRequest)

      Then("Result should be with status Bad request with expected json as body")
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.parse(expectedJson)
    }
  }

  "be failed because of not existed project to update" in {
    Given("Data needed to prepare request, expected result")
    val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" +
      ".eyJhdXRob3JfaWQiOiJlNTRlNTY5Mi02MGQzLTRjODQtYTI1MS02NmFhOTk4ZDdjYjEiLCJwcm9qZWN0X2lkX29sZCI6InVuaXF1ZV9wcm9qZWN0X2lkXzEiLCJwcm9qZWN0X2lkX25ldyI6Im5ld191bmlxdWVfcHJvamVjdF9pZF8xIn0" +
      ".Jf5ELYPYZvQyPYr8AdmjvRPpfcOdSFRW8jMJEm7Vfss"

    val expectedJson = """
      {
        "success": false,
        "message": "Provided project could not be found",
        "data": ""
      }
      """

    val config = mock[Configuration]
    when(config.get[String]("secret.key")).thenReturn("Test secret key")

    val projectIdOld = "unique_project_id_1"
    val projectIdNew = "new_unique_project_id_1"

    val repository = mock[ProjectRepository]
    when(repository.find(projectIdNew)).thenReturn(Future.successful(Option.empty))
    when(repository.find(projectIdOld)).thenReturn(Future.successful(Option.empty))

    val projectAggregate = new ProjectAggregate(repository)
    val authHandler = AuthenticationHandler(config)
    val givenRequest = FakeRequest()
      .withMethod(POST)
      .withHeaders((AUTHORIZATION, bearer))

    When("Update method is performed")
    val controller = new ProjectController(Helpers.stubControllerComponents(), projectAggregate, authHandler)
    val result: Future[Result] = controller.update().apply(givenRequest)

    Then("Result should be with status Bad request with expected json as body")
    status(result) mustBe BAD_REQUEST
    contentAsJson(result) mustBe Json.parse(expectedJson)
  }

  "be failed because of updated project is not by the author" in {
    Given("Data needed to prepare request, expected result")
    val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" +
      ".eyJhdXRob3JfaWQiOiJlNTRlNTY5Mi02MGQzLTRjODQtYTI1MS02NmFhOTk4ZDdjYjEiLCJwcm9qZWN0X2lkX29sZCI6InVuaXF1ZV9wcm9qZWN0X2lkXzEiLCJwcm9qZWN0X2lkX25ldyI6Im5ld191bmlxdWVfcHJvamVjdF9pZF8xIn0" +
      ".Jf5ELYPYZvQyPYr8AdmjvRPpfcOdSFRW8jMJEm7Vfss"

    val expectedJson = """
      {
        "success": false,
        "message": "Provided project could not be found",
        "data": ""
      }
      """

    val config = mock[Configuration]
    when(config.get[String]("secret.key")).thenReturn("Test secret key")

    val projectIdOld = "unique_project_id_1"
    val projectIdNew = "new_unique_project_id_1"

    val repository = mock[ProjectRepository]
    when(repository.find(projectIdNew)).thenReturn(Future.successful(Option.empty))
    when(repository.find(projectIdOld)).thenReturn(Future.successful(Option.empty))

    val projectAggregate = new ProjectAggregate(repository)
    val authHandler = AuthenticationHandler(config)
    val givenRequest = FakeRequest()
      .withMethod(POST)
      .withHeaders((AUTHORIZATION, bearer))

    When("Update method is performed")
    val controller = new ProjectController(Helpers.stubControllerComponents(), projectAggregate, authHandler)
    val result: Future[Result] = controller.update().apply(givenRequest)

    Then("Result should be with status Bad request with expected json as body")
    status(result) mustBe BAD_REQUEST
    contentAsJson(result) mustBe Json.parse(expectedJson)
  }
}

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
import play.api.http.Status.{BAD_REQUEST, CREATED, INTERNAL_SERVER_ERROR, OK}
import play.api.http.{ContentTypes, HttpEntity}
import play.api.libs.json.Json
import play.api.mvc.{ResponseHeader, Result}
import play.api.test.Helpers.{AUTHORIZATION, POST, PUT, contentAsJson, defaultAwaitTimeout, status}
import play.api.test.{FakeRequest, Helpers}
import project.commands.{CreateProjectCommand, DeleteProjectCommand, UpdateProjectCommand}
import project.{Project, ProjectAggregate, ProjectRepository}
import task.TaskRepository

import java.time.LocalDateTime
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

      val projectRepository = mock[ProjectRepository]
      val taskRepository = mock[TaskRepository]

      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
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

      val projectRepository = mock[ProjectRepository]
      val taskRepository = mock[TaskRepository]

      val body = HttpEntity.Strict(ByteString(expectedJson), Some(ContentTypes.JSON))
      val response = Result(ResponseHeader(CREATED), body)

      when(projectRepository.find(projectId)).thenReturn(Future.successful(Option.empty))
      when(projectRepository.create(CreateProjectCommand(authorId, projectId))).thenReturn(Future.successful(response))

      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
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

      val projectRepository = mock[ProjectRepository]
      val taskRepository = mock[TaskRepository]

      when(projectRepository.find(projectId)).thenReturn(Future.successful(Some(Project(authorId, projectId))))

      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
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

      val projectRepository = mock[ProjectRepository]
      val taskRepository = mock[TaskRepository]

      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
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

      val projectRepository = mock[ProjectRepository]
      val taskRepository = mock[TaskRepository]

      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
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

      val projectRepository = mock[ProjectRepository]
      val taskRepository = mock[TaskRepository]

      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
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

      val projectRepository = mock[ProjectRepository]
      val taskRepository = mock[TaskRepository]

      when(projectRepository.find(projectIdNew)).thenReturn(Future.successful(Option.empty))
      when(projectRepository.find(projectIdOld)).thenReturn(Future.successful(Some(Project(authorId, projectIdOld))))
      when(projectRepository.find(projectIdOld, authorId)).thenReturn(Future.successful(Some(Project(authorId, projectIdOld))))
      when(projectRepository.update(UpdateProjectCommand(authorId, projectIdOld, projectIdNew)))
        .thenReturn(Future.successful(response))

      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
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

      val projectRepository = mock[ProjectRepository]
      val taskRepository = mock[TaskRepository]

      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
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

      val projectRepository = mock[ProjectRepository]
      val taskRepository = mock[TaskRepository]

      when(projectRepository.find(projectIdNew)).thenReturn(Future.successful(Option.empty))
      when(projectRepository.find(projectIdOld)).thenReturn(Future.successful(Some(Project(authorId, projectIdOld))))
      when(projectRepository.find(projectIdOld, authorId)).thenReturn(Future.successful(Option.empty))

      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
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

      val projectRepository = mock[ProjectRepository]
      val taskRepository = mock[TaskRepository]

      when(projectRepository.find(projectIdNew)).thenReturn(Future.successful(Some(Project(authorId, projectIdNew))))

      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
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

    val projectRepository = mock[ProjectRepository]
    val taskRepository = mock[TaskRepository]

    when(projectRepository.find(projectIdNew)).thenReturn(Future.successful(Option.empty))
    when(projectRepository.find(projectIdOld)).thenReturn(Future.successful(Option.empty))

    val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
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

    val projectRepository = mock[ProjectRepository]
    val taskRepository = mock[TaskRepository]

    when(projectRepository.find(projectIdNew)).thenReturn(Future.successful(Option.empty))
    when(projectRepository.find(projectIdOld)).thenReturn(Future.successful(Option.empty))

    val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
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

  "ProjectController#delete" should {
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

      val projectRepository = mock[ProjectRepository]
      val taskRepository = mock[TaskRepository]

      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(POST)
        .withHeaders((AUTHORIZATION, bearer))

      When("Delete method is performed")
      val controller = new ProjectController(Helpers.stubControllerComponents(), projectAggregate, authHandler)
      val result: Future[Result] = controller.delete().apply(givenRequest)

      Then("Result should be with status Bad request with expected json as body")
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.parse(expectedJson)
    }

    "be successful" in {
      Given("Data needed to prepare request, expected result")
      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJwcm9qZWN0X2lkIjoic2llbWEtc2llbWEiLCJhdXRob3JfaWQiO" +
        "iJkMTA4OWUyZi01NjQ5LTQ5MzMtODM1Ny03MWQyZjZiYzEzNjIifQ.k2kdnkNAVR3TIDgLtPOeXrwzEhjwfrr2UOn6kl8DnEM"

      val givenProjectDeleteJson = """
      {
        "success": true,
        "message": "Project deleted",
        "data": {
          "project_id": "siema-siema",
          "author_id": "d1089e2f-5649-4933-8357-71d2f6bc1362"
        }
      }
      """

      val givenTasksDeletedJson = """
      {
        "success": true,
        "message": "Tasks deleted",
        "data": {
          "project_id": "siema-siema",
          "author_id": "d1089e2f-5649-4933-8357-71d2f6bc1362"
        }
      }
      """

      val expectedJson = """
      {
        "success": true,
        "message": "Project deleted",
        "data": {
          "project_id": "siema-siema",
          "author_id": "d1089e2f-5649-4933-8357-71d2f6bc1362"
        }
      }
      """

      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val authorId = UUID("d1089e2f-5649-4933-8357-71d2f6bc1362")
      val projectId = "siema-siema"

      val bodyProjectDeleted = HttpEntity.Strict(ByteString(givenProjectDeleteJson), Some(ContentTypes.JSON))
      val responseProjectDeleted = Result(ResponseHeader(OK), bodyProjectDeleted)

      val bodyTasksDeleted = HttpEntity.Strict(ByteString(givenTasksDeletedJson), Some(ContentTypes.JSON))
      val responseTasksDeleted = Result(ResponseHeader(OK), bodyTasksDeleted)

      val projectRepository = mock[ProjectRepository]
      val taskRepository = mock[TaskRepository]

      when(projectRepository.find(projectId))
        .thenReturn(Future.successful(Some(Project(authorId, projectId))))
      when(projectRepository.delete(DeleteProjectCommand(authorId, projectId)))
        .thenReturn(Future.successful(responseProjectDeleted))
      when(taskRepository.deleteAll(projectId))
        .thenReturn(Future.successful(responseTasksDeleted))

      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(PUT)
        .withHeaders((AUTHORIZATION, bearer))

      When("Delete method is performed")
      val controller = new ProjectController(Helpers.stubControllerComponents(), projectAggregate, authHandler)
      val result: Future[Result] = controller.delete().apply(givenRequest)

      Then("Result should be with status Ok with expected json as body")
      status(result) mustBe OK
      contentAsJson(result) mustBe Json.parse(expectedJson)
    }

    "be failed because of empty project" in {
      Given("Data needed to prepare request, expected result")
      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdXRob3JfaWQiOiJkMTA4OWUyZi01NjQ5LTQ5MzMtODM1Ny03M" +
        "WQyZjZiYzEzNjIifQ.5IidfqLK4_cBPixho4uB-Uojh8Lcd9UFHN6M8CYgNHk"

      val expectedJson = """
      {
        "success": false,
        "message": "Provided project id is empty",
        "data": ""
      }
      """

      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val projectId = "siema-siema"

      val projectRepository = mock[ProjectRepository]
      val taskRepository = mock[TaskRepository]

      when(projectRepository.find(projectId))
        .thenReturn(Future.successful(None))

      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(PUT)
        .withHeaders((AUTHORIZATION, bearer))

      When("Delete method is performed")
      val controller = new ProjectController(Helpers.stubControllerComponents(), projectAggregate, authHandler)
      val result: Future[Result] = controller.delete().apply(givenRequest)

      Then("Result should be with status Bad request with expected json as body")
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.parse(expectedJson)
    }

    "be failed because of not valid author id" in {
      Given("Data needed to prepare request, expected result")
      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJwcm9qZWN0X2lkIjoic2llbWEtc2llbWEiLCJhdXRob3JfaW" +
        "QiOiJmamdkZmhnZGZnIn0.kjpKQPVNRlE4tQWzNkDGLIAKaVyCC-JVNfdjbTaIn8k"

      val expectedJson = """
      {
        "success": false,
        "message": "Provided author id is not valid",
        "data": ""
      }
      """

      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val projectId = "siema-siema"

      val projectRepository = mock[ProjectRepository]
      val taskRepository = mock[TaskRepository]

      when(projectRepository.find(projectId))
        .thenReturn(Future.successful(None))

      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(PUT)
        .withHeaders((AUTHORIZATION, bearer))

      When("Delete method is performed")
      val controller = new ProjectController(Helpers.stubControllerComponents(), projectAggregate, authHandler)
      val result: Future[Result] = controller.delete().apply(givenRequest)

      Then("Result should be with status Bad request with expected json as body")
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.parse(expectedJson)
    }

    "be failed because of project not found" in {
      Given("Data needed to prepare request, expected result")
      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJwcm9qZWN0X2lkIjoic2llbWEtc2llbWEiLCJhdXRob3JfaWQi" +
        "OiJkMTA4OWUyZi01NjQ5LTQ5MzMtODM1Ny03MWQyZjZiYzEzNjIifQ.k2kdnkNAVR3TIDgLtPOeXrwzEhjwfrr2UOn6kl8DnEM"

      val expectedJson = """
      {
        "success": false,
        "message": "Provided project could not be found",
        "data": ""
      }
      """

      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val projectId = "siema-siema"

      val projectRepository = mock[ProjectRepository]
      val taskRepository = mock[TaskRepository]

      when(projectRepository.find(projectId))
        .thenReturn(Future.successful(None))

      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(PUT)
        .withHeaders((AUTHORIZATION, bearer))

      When("Delete method is performed")
      val controller = new ProjectController(Helpers.stubControllerComponents(), projectAggregate, authHandler)
      val result: Future[Result] = controller.delete().apply(givenRequest)

      Then("Result should be with status Bad request with expected json as body")
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.parse(expectedJson)
    }

    "be failed because of author is not the author of the project" in {
      Given("Data needed to prepare request, expected result")
      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJwcm9qZWN0X2lkIjoic2llbWEtc2llbWEiLCJhdXRob3JfaWQ" +
        "iOiI2YWMyYzljOC0xZjYwLTQ0ZDctOWUwZC02ZDhmODA0MWMxMDAifQ.gZyw_2XPf-Bfv1fkiZ4b23ql1giGMl6ooHQItxeQ0mo"

      val expectedJson = """
      {
        "success": false,
        "message": "Provided author is not author of the project",
        "data": ""
      }
      """

      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val authorId = UUID("d1089e2f-5649-4933-8357-71d2f6bc1362")
      val projectId = "siema-siema"

      val projectRepository = mock[ProjectRepository]
      val taskRepository = mock[TaskRepository]

      when(projectRepository.find(projectId))
        .thenReturn(Future.successful(Some(Project(authorId, projectId))))

      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(PUT)
        .withHeaders((AUTHORIZATION, bearer))

      When("Delete method is performed")
      val controller = new ProjectController(Helpers.stubControllerComponents(), projectAggregate, authHandler)
      val result: Future[Result] = controller.delete().apply(givenRequest)

      Then("Result should be with status Bad request with expected json as body")
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.parse(expectedJson)
    }

    "be failed because of project is already deleted" in {
      Given("Data needed to prepare request, expected result")
      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJwcm9qZWN0X2lkIjoic2llbWEtc2llbWEiLCJhdXRob3JfaWQiO" +
        "iJkMTA4OWUyZi01NjQ5LTQ5MzMtODM1Ny03MWQyZjZiYzEzNjIifQ.k2kdnkNAVR3TIDgLtPOeXrwzEhjwfrr2UOn6kl8DnEM"

      val expectedJson = """
      {
        "success": false,
        "message": "Project to delete is already deleted",
        "data": ""
      }
      """

      val deleted: LocalDateTime = LocalDateTime.of(2022, 1, 2, 12, 12)

      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val authorId = UUID("d1089e2f-5649-4933-8357-71d2f6bc1362")
      val projectId = "siema-siema"

      val projectRepository = mock[ProjectRepository]
      val taskRepository = mock[TaskRepository]

      when(projectRepository.find(projectId))
        .thenReturn(Future.successful(Some(Project(authorId, projectId, Option(deleted)))))

      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(PUT)
        .withHeaders((AUTHORIZATION, bearer))

      When("Delete method is performed")
      val controller = new ProjectController(Helpers.stubControllerComponents(), projectAggregate, authHandler)
      val result: Future[Result] = controller.delete().apply(givenRequest)

      Then("Result should be with status Bad request with expected json as body")
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.parse(expectedJson)
    }

    "be failed because of failed task delete" in {
      Given("Data needed to prepare request, expected result")
      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJwcm9qZWN0X2lkIjoic2llbWEtc2llbWEiLCJhdXRob3JfaWQiO" +
        "iJkMTA4OWUyZi01NjQ5LTQ5MzMtODM1Ny03MWQyZjZiYzEzNjIifQ.k2kdnkNAVR3TIDgLtPOeXrwzEhjwfrr2UOn6kl8DnEM"

      val givenProjectDeleteJson = """
      {
        "success": true,
        "message": "Project deleted",
        "data": {
          "project_id": "siema-siema",
          "author_id": "d1089e2f-5649-4933-8357-71d2f6bc1362"
        }
      }
      """

      val givenTasksDeletedJson = """
      {
        "success": false,
        "message": "Database error",
        "data": ""
      }
      """

      val expectedJson = """
      {
        "success": false,
        "message": "Project delete failed",
        "data": ""
      }
      """

      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val authorId = UUID("d1089e2f-5649-4933-8357-71d2f6bc1362")
      val projectId = "siema-siema"

      val bodyProjectDeleted = HttpEntity.Strict(ByteString(givenProjectDeleteJson), Some(ContentTypes.JSON))
      val responseProjectDeleted = Result(ResponseHeader(OK), bodyProjectDeleted)

      val bodyTasksDeleted = HttpEntity.Strict(ByteString(givenTasksDeletedJson), Some(ContentTypes.JSON))
      val responseTasksDeleted = Result(ResponseHeader(INTERNAL_SERVER_ERROR), bodyTasksDeleted)

      val projectRepository = mock[ProjectRepository]
      val taskRepository = mock[TaskRepository]

      when(projectRepository.find(projectId))
        .thenReturn(Future.successful(Some(Project(authorId, projectId))))
      when(projectRepository.delete(DeleteProjectCommand(authorId, projectId)))
        .thenReturn(Future.successful(responseProjectDeleted))
      when(taskRepository.deleteAll(projectId))
        .thenReturn(Future.successful(responseTasksDeleted))

      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(PUT)
        .withHeaders((AUTHORIZATION, bearer))

      When("Delete method is performed")
      val controller = new ProjectController(Helpers.stubControllerComponents(), projectAggregate, authHandler)
      val result: Future[Result] = controller.delete().apply(givenRequest)

      Then("Result should be with status Bad request with expected json as body")
      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe Json.parse(expectedJson)
    }
  }
}

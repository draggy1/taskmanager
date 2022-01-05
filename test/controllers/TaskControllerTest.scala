package controllers

import akka.util.ByteString
import authentication.AuthenticationHandler
import common.StringUtils
import io.jvm.uuid.UUID
import org.bson.types.ObjectId
import org.mockito.Mockito.when
import org.scalatest.GivenWhenThen
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.http.Status.{BAD_REQUEST, CREATED}
import play.api.http.{ContentTypes, HttpEntity}
import play.api.libs.json.Json
import play.api.mvc.{ResponseHeader, Result}
import play.api.test.Helpers.{AUTHORIZATION, DELETE, POST, contentAsJson, defaultAwaitTimeout, status}
import play.api.test.{FakeRequest, Helpers}
import project.{Project, ProjectAggregate, ProjectRepository}
import task._
import task.commands.CreateTaskCommand
import task.queries.{GetTaskByProjectIdAndStartQuery, GetTaskByProjectIdAndTimeDetailsQuery}

import java.time.LocalDateTime
import scala.concurrent.Future

class TaskControllerTest extends PlaySpec with MockitoSugar with GivenWhenThen with ScalaFutures {

  private implicit val defaultPatience: PatienceConfig = {
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))
  }

  "TaskController#create" should {
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

      val taskRepository = mock[TaskRepository]
      val projectRepository = mock[ProjectRepository]

      val taskAggregate = new TaskAggregate(taskRepository)
      val projectAggregate = new ProjectAggregate(projectRepository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(POST)
        .withHeaders((AUTHORIZATION, bearer))

      When("Creation method is performed")
      val controller = new TaskController(Helpers.stubControllerComponents(), taskAggregate, projectAggregate, authHandler)
      val result: Future[Result] = controller.create().apply(givenRequest)

      Then("Result should be with status Bad Request with expected json as body")
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.parse(expectedJson)
    }

    "be successful" in {
      Given("Data needed to prepare request, expected result")
      val projectId = "unique_project_id_2"
      val start = LocalDateTime.of(2021, 12, 23, 13, 0, 0)
      val duration = TaskDuration(2, 32)
      val end = LocalDateTime.of(2021, 12, 23, 15, 32, 0)
      val timeDetails = TaskTimeDetails(start, end, duration)
      val authorId = UUID("e54e5692-60d3-4c84-a251-66aa998d7cb1")

      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJwcm9qZWN0X2lkIjoidW5pcXVlX3Byb2plY3RfaWRfMiIsImF" +
        "1dGhvcl9pZCI6ImU1NGU1NjkyLTYwZDMtNGM4NC1hMjUxLTY2YWE5OThkN2NiMSIsInN0YXJ0X2RhdGUiOiIyMy0xMi0yMDIxIDEzOjAwI" +
        "iwiZHVyYXRpb24iOiIyIGhvdXJzIDMyIG1pbnV0ZXMiLCJ2b2x1bWUiOjQzLCJjb21tZW50IjoiUmFuZG9tIGNvbW1lbnQifQ.1a4plTmN" +
        "PjW2kQBOkuf2quvCpnHQg6HQ0uUctR1GqtI"

      val expectedJson =
        """
      {
        "success":true,
        "message":"Task created",
        "data": {
          "project_id":"unique_project_id_2",
          "author_id":"e54e5692-60d3-4c84-a251-66aa998d7cb1",
          "task_time_details": {
            "start_date":"2021-12-23T13:00:00",
            "duration": {
              "hours":2,
              "minutes":32
             }
          },
        "volume":43,
        "comment":"Random comment"}
      }
      """

      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val taskRepository = mock[TaskRepository]
      val projectRepository = mock[ProjectRepository]
      val body = HttpEntity.Strict(ByteString(expectedJson), Some(ContentTypes.JSON))
      val response = Result(ResponseHeader(CREATED), body)
      val query = GetTaskByProjectIdAndTimeDetailsQuery(projectId, timeDetails)

      when(taskRepository.find(query)).thenReturn(Future.successful(Option.empty))
      when(projectRepository.find(projectId, authorId))
        .thenReturn(Future.successful(Option(Project(authorId, projectId))))
      when(taskRepository.create(CreateTaskCommand(projectId, authorId, timeDetails, Option(43), Option("Random comment"))))
        .thenReturn(Future.successful(response))

      val taskAggregate = new TaskAggregate(taskRepository)
      val projectAggregate = new ProjectAggregate(projectRepository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(POST)
        .withHeaders((AUTHORIZATION, bearer))

      When("Creation method is performed")
      val controller = new TaskController(Helpers.stubControllerComponents(), taskAggregate, projectAggregate, authHandler)
      val result: Future[Result] = controller.create().apply(givenRequest)

      Then("Result should be with status Created with expected json as body")
      status(result) mustBe CREATED
      contentAsJson(result) mustBe Json.parse(expectedJson)
    }

    "returns error response because of empty project id" in {
      Given("Data needed to prepare request, expected result")

      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdGFydF9kYXRlIjoiMjMtMTItMjAyMSAxMzowMCIsImR1cmF0a" +
        "W9uIjoiMiBob3VycyAzMiBtaW51dGVzIiwidm9sdW1lIjo0MywiY29tbWVudCI6IlJhbmRvbSBjb21tZW50In0.OW1x9grkSaH7d679kwukl" +
        "UByPHQA5_O9UPOoj_WVFss"

      val expectedJson =
        """
      {
        "success":false,
        "message":"Provided project id is empty",
        "data": ""
      }
      """

      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val taskRepository = mock[TaskRepository]
      val projectRepository = mock[ProjectRepository]

      val taskAggregate = new TaskAggregate(taskRepository)
      val projectAggregate = new ProjectAggregate(projectRepository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(POST)
        .withHeaders((AUTHORIZATION, bearer))

      When("Creation method is performed")
      val controller = new TaskController(Helpers.stubControllerComponents(), taskAggregate, projectAggregate, authHandler)
      val result: Future[Result] = controller.create().apply(givenRequest)

      Then("Result should be with status Bad request with expected json as body")
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.parse(expectedJson)
    }

    "returns error response because of not proper start date" in {
      Given("Data needed to prepare request, expected result")
      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJwcm9qZWN0X2lkIjoidW5pcXVlX3Byb2plY3RfaWRfMiIsInN0" +
        "YXJ0X2RhdGUiOiIyMy4xMi4yMDIxIDEzOjAwIiwiZHVyYXRpb24iOiIyIGhvdXJzIDMyIG1pbnV0ZXMiLCJ2b2x1bWUiOjQzLCJjb21tZW5" +
        "0IjoiUmFuZG9tIGNvbW1lbnQifQ.ro0bd77PPpg-JTRAofvoiQAJr4qHzD1HTUPtDMlj83A"

      val expectedJson =
        """
      {
        "success":false,
        "message":"Provided date is incorrect",
        "data": ""
      }
      """

      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val taskRepository = mock[TaskRepository]
      val projectRepository = mock[ProjectRepository]

      val taskAggregate = new TaskAggregate(taskRepository)
      val projectAggregate = new ProjectAggregate(projectRepository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(POST)
        .withHeaders((AUTHORIZATION, bearer))

      When("Creation method is performed")
      val controller = new TaskController(Helpers.stubControllerComponents(), taskAggregate, projectAggregate, authHandler)
      val result: Future[Result] = controller.create().apply(givenRequest)

      Then("Result should be with status Created with expected json as body")
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.parse(expectedJson)
    }

    "returns error response because of not proper duration" in {
      Given("Data needed to prepare request, expected result")

      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJwcm9qZWN0X2lkIjoicHJvamVjdF9pZF9uZXciLCJzdGFydF9k" +
        "YXRlIjoiMjMtMTItMjAyMSAxMzowMCIsImR1cmF0aW9uIjoiZ2hqZmdqaGlmZyIsInZvbHVtZSI6NDMsImNvbW1lbnQiOiJSYW5kb20gY29" +
        "tbWVudCJ9.Ww57rCdgfywrkp6dFbZC2-AbiktbjhZFfjffobQo1zg"

      val expectedJson =
        """
      {
        "success":false,
        "message":"Provided duration is incorrect",
        "data": ""
      }
      """

      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val taskRepository = mock[TaskRepository]
      val projectRepository = mock[ProjectRepository]

      val taskAggregate = new TaskAggregate(taskRepository)
      val projectAggregate = new ProjectAggregate(projectRepository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(POST)
        .withHeaders((AUTHORIZATION, bearer))

      When("Creation method is performed")
      val controller = new TaskController(Helpers.stubControllerComponents(), taskAggregate, projectAggregate, authHandler)
      val result: Future[Result] = controller.create().apply(givenRequest)

      Then("Result should be with status Created with expected json as body")
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.parse(expectedJson)
    }

    "returns error response because of existing task is in conflict" in {
      Given("Data needed to prepare request, expected result")
      val projectId = "unique_project_id_2"
      val start = LocalDateTime.of(2021, 12, 23, 13, 0, 0)
      val duration = TaskDuration(2, 32)
      val end = LocalDateTime.of(2021, 12, 23, 15, 32, 0)
      val timeDetails = TaskTimeDetails(start, end, duration)

      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJwcm9qZWN0X2lkIjoidW5pcXVlX3Byb2plY3RfaWRfMiIsInN" +
        "0YXJ0X2RhdGUiOiIyMy0xMi0yMDIxIDEzOjAwIiwiZHVyYXRpb24iOiIyIGhvdXJzIDMyIG1pbnV0ZXMiLCJ2b2x1bWUiOjQzLCJjb21tZ" +
        "W50IjoiUmFuZG9tIGNvbW1lbnQifQ.VLBZBiTAMu-3WMEu6rSL0MpshGpuSVwSircoIs1iTqM"

      val expectedJson =
        """
      {
        "success":false,
        "message":"Provided task is in conflict with another task",
        "data": ""
      }
      """

      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val taskRepository = mock[TaskRepository]
      val projectRepository = mock[ProjectRepository]
      val query = GetTaskByProjectIdAndTimeDetailsQuery(projectId, timeDetails)

      when(taskRepository.find(query))
        .thenReturn(Future.successful(Option(prepareGivenTask(projectId))))

      val taskAggregate = new TaskAggregate(taskRepository)
      val projectAggregate = new ProjectAggregate(projectRepository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(POST)
        .withHeaders((AUTHORIZATION, bearer))

      When("Creation method is performed")
      val controller = new TaskController(Helpers.stubControllerComponents(), taskAggregate, projectAggregate, authHandler)
      val result: Future[Result] = controller.create().apply(givenRequest)

      Then("Result should be with status Created with expected json as body")
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.parse(expectedJson)
    }

    "returns an error response because the specified project to which the task is created does not exist" in {
      Given("Data needed to prepare request, expected result")
      val projectId = "unique_project_id_2"
      val start = LocalDateTime.of(2021, 12, 23, 13, 0, 0)
      val duration = TaskDuration(2, 32)
      val end = LocalDateTime.of(2021, 12, 23, 15, 32, 0)
      val timeDetails = TaskTimeDetails(start, end, duration)
      val authorId = UUID("ea2550d0-2272-470e-8265-00f2946dc817")

      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJwcm9qZWN0X2lkIjoidW5pcXVlX3Byb2plY3RfaWRfMiIsImF" +
        "1dGhvcl9pZCI6ImVhMjU1MGQwLTIyNzItNDcwZS04MjY1LTAwZjI5NDZkYzgxNyIsInN0YXJ0X2RhdGUiOiIyMy0xMi0yMDIxIDEzOjAwIi" +
        "wiZHVyYXRpb24iOiIyIGhvdXJzIDMyIG1pbnV0ZXMiLCJ2b2x1bWUiOjQzLCJjb21tZW50IjoiUmFuZG9tIGNvbW1lbnQifQ.3B4f9RCgJ" +
        "RxnN-pGgc5Ecw2ow8d9StzLaLd5sCkvyZo"

      val expectedJson =
        """
      {
        "success":false,
        "message":"Provided project could not be found",
        "data": ""
      }
      """

      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val taskRepository = mock[TaskRepository]
      val projectRepository = mock[ProjectRepository]
      val query = GetTaskByProjectIdAndTimeDetailsQuery(projectId, timeDetails)

      when(taskRepository.find(query)).thenReturn(Future.successful(Option.empty))
      when(projectRepository.find(projectId, authorId))
        .thenReturn(Future.successful(Option.empty))

      val taskAggregate = new TaskAggregate(taskRepository)
      val projectAggregate = new ProjectAggregate(projectRepository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(POST)
        .withHeaders((AUTHORIZATION, bearer))

      When("Creation method is performed")
      val controller = new TaskController(Helpers.stubControllerComponents(), taskAggregate, projectAggregate, authHandler)
      val result: Future[Result] = controller.create().apply(givenRequest)

      Then("Result should be with status Created with expected json as body")
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.parse(expectedJson)
    }
  }

  "TaskController#delete" should {
    "be successful" in {
      Given("Data needed to prepare request, expected result")
      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJwcm9qZWN0X2lkIjoicHJvamVjdF9pZCIsIn" +
        "N0YXJ0X2RhdGUiOiIyMC0wNi0yMDIxIDA4OjAzIn0.v5Qmp0rIxIqpwNkkq2p00lRYAwyZsmgrnGmd_46SYaM"

      val expectedJson =
        """
      {
        "success":false,
        "message":"Provided author id is not valid",
        "data": ""
      }
      """
      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val taskRepository = mock[TaskRepository]
      val projectRepository = mock[ProjectRepository]

      val taskAggregate = new TaskAggregate(taskRepository)
      val projectAggregate = new ProjectAggregate(projectRepository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(DELETE)
        .withHeaders((AUTHORIZATION, bearer))

      When("Delete method is performed")
      val controller = new TaskController(Helpers.stubControllerComponents(), taskAggregate, projectAggregate, authHandler)
      val result: Future[Result] = controller.delete().apply(givenRequest)

      Then("Result should be with status Ok with expected json as body")
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.parse(expectedJson)
    }

    "be failed because of incorrect bearer" in {
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

      val taskRepository = mock[TaskRepository]
      val projectRepository = mock[ProjectRepository]

      val taskAggregate = new TaskAggregate(taskRepository)
      val projectAggregate = new ProjectAggregate(projectRepository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(DELETE)
        .withHeaders((AUTHORIZATION, bearer))

      When("Delete method is performed")
      val controller = new TaskController(Helpers.stubControllerComponents(), taskAggregate, projectAggregate, authHandler)
      val result: Future[Result] = controller.delete().apply(givenRequest)

      Then("Result should be with status Ok with expected json as body")
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.parse(expectedJson)
    }

    "be failed because of empty project id" in {
      Given("Data needed to prepare request, expected result")

      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdXRob3JfaWQiOiJlNTRlNTY5Mi02MGQzLTRjODQtYTI1MS02NmF" +
        "hOTk4ZDdjYjIiLCJzdGFydF9kYXRlIjoiMjAtMDYtMjAyMSAwODowMyJ9._MX1IkQqj2wuH9V9XH8dsLGxmRP157XrjdbfICjb-Wo"

      val expectedJson =
        """
      {
        "success":false,
        "message":"Provided project id is empty",
        "data": ""
      }
      """
      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val taskRepository = mock[TaskRepository]
      val projectRepository = mock[ProjectRepository]

      val taskAggregate = new TaskAggregate(taskRepository)
      val projectAggregate = new ProjectAggregate(projectRepository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(DELETE)
        .withHeaders((AUTHORIZATION, bearer))

      When("Delete method is performed")
      val controller = new TaskController(Helpers.stubControllerComponents(), taskAggregate, projectAggregate, authHandler)
      val result: Future[Result] = controller.delete().apply(givenRequest)

      Then("Result should be with status 'Bad request' with expected json as body")
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.parse(expectedJson)
    }

    "be failed because of empty author id" in {
      Given("Data needed to prepare request, expected result")

      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdXRob3JfaWQiOiJlNTRlNTY5Mi02MGQzLTRjODQtYTI1MS02NmF" +
        "hOTk4ZDdjYjIiLCJzdGFydF9kYXRlIjoiMjAtMDYtMjAyMSAwODowMyJ9._MX1IkQqj2wuH9V9XH8dsLGxmRP157XrjdbfICjb-Wo"

      val expectedJson =
        """
      {
        "success":false,
        "message":"Provided project id is empty",
        "data": ""
      }
      """
      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val taskRepository = mock[TaskRepository]
      val projectRepository = mock[ProjectRepository]

      val taskAggregate = new TaskAggregate(taskRepository)
      val projectAggregate = new ProjectAggregate(projectRepository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(DELETE)
        .withHeaders((AUTHORIZATION, bearer))

      When("Delete method is performed")
      val controller = new TaskController(Helpers.stubControllerComponents(), taskAggregate, projectAggregate, authHandler)
      val result: Future[Result] = controller.delete().apply(givenRequest)

      Then("Result should be with status 'Bad request' with expected json as body")
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.parse(expectedJson)
    }

    "be failed because of not valid author id" in {
      Given("Data needed to prepare request, expected result")

      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJwcm9qZWN0X2lkIjoicHJvamVjdF9pZCIsImF1dGhvcl9pZCI6InNsa2pk" +
        "Z2pkc2Znc2hrZGpmaGtqZGdmaGtqZ2RmZGdmaGtqIiwic3RhcnRfZGF0ZSI6IjIwLTA2LTIwMjEgMDg6MDMifQ.WYhY5dvcYA5Kk65KCepMMj4gv_iQ" +
        "gyW1-kNd0_gLJVk"

      val expectedJson =
        """
      {
        "success":false,
        "message":"Provided author id is not valid",
        "data": ""
      }
      """

      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val taskRepository = mock[TaskRepository]
      val projectRepository = mock[ProjectRepository]

      val taskAggregate = new TaskAggregate(taskRepository)
      val projectAggregate = new ProjectAggregate(projectRepository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(DELETE)
        .withHeaders((AUTHORIZATION, bearer))

      When("Delete method is performed")
      val controller = new TaskController(Helpers.stubControllerComponents(), taskAggregate, projectAggregate, authHandler)
      val result: Future[Result] = controller.delete().apply(givenRequest)

      Then("Result should be with status 'Bad Request' with expected json as body")
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.parse(expectedJson)
    }

    "be failed because incorrect date" in {
      Given("Data needed to prepare request, expected result")

      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJwcm9qZWN0X2lkIjoicHJvamVjdF9pZCIsImF1dGhvcl9pZCI6ImU1NG" +
        "U1NjkyLTYwZDMtNGM4NC1hMjUxLTY2YWE5OThkN2NiMiIsInN0YXJ0X2RhdGUiOiIyMC4wNi4yMDIxIDA4OjAzIn0.4_GbwaLHo96kHRwCHwbM01i" +
        "LbG_oB-JyonW6swHrrvQ"

      val expectedJson =
        """
      {
        "success":false,
        "message":"Provided date is incorrect",
        "data": ""
      }
      """

      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val taskRepository = mock[TaskRepository]
      val projectRepository = mock[ProjectRepository]

      val taskAggregate = new TaskAggregate(taskRepository)
      val projectAggregate = new ProjectAggregate(projectRepository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(DELETE)
        .withHeaders((AUTHORIZATION, bearer))

      When("Delete method is performed")
      val controller = new TaskController(Helpers.stubControllerComponents(), taskAggregate, projectAggregate, authHandler)
      val result: Future[Result] = controller.delete().apply(givenRequest)

      Then("Result should be with status Bad request with expected json as body")
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.parse(expectedJson)
    }

    "be failed because of not existing task to delete" in {
      Given("Data needed to prepare request, expected result")
      val projectId = "project_id"
      val start = LocalDateTime.of(2021, 6, 20, 8, 3, 0)

      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJwcm9qZWN0X2lkIjoicHJvamVjdF9pZCIsImF1dGhvcl9pZCI" +
        "6ImU1NGU1NjkyLTYwZDMtNGM4NC1hMjUxLTY2YWE5OThkN2NiMiIsInN0YXJ0X2RhdGUiOiIyMC0wNi0yMDIxIDA4OjAzIn0.hUoWoKx7D" +
        "GxDOA3I9Ub335wHiC6ZiR8z-gHBv1DTESM"

      val expectedJson =
        """
      {
        "success":false,
        "message":"Task not exist for provided project id and start",
        "data": ""
      }
      """

      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val taskRepository = mock[TaskRepository]
      val projectRepository = mock[ProjectRepository]

      val taskQuery = GetTaskByProjectIdAndStartQuery(projectId, start)

      when(taskRepository.find(taskQuery))
        .thenReturn(Future.successful(Option.empty))

      val taskAggregate = new TaskAggregate(taskRepository)
      val projectAggregate = new ProjectAggregate(projectRepository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(DELETE)
        .withHeaders((AUTHORIZATION, bearer))

      When("Delete method is performed")
      val controller = new TaskController(Helpers.stubControllerComponents(), taskAggregate, projectAggregate, authHandler)
      val result: Future[Result] = controller.delete().apply(givenRequest)

      Then("Result should be with status Ok with expected json as body")
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.parse(expectedJson)
    }

    "be failed because of author is not the author of the project" in {
      Given("Data needed to prepare request, expected result")
      val projectId = "project_id"
      val start = LocalDateTime.of(2021, 6, 20, 8, 3, 0)
      val authorId = UUID("e54e5692-60d3-4c84-a251-66aa998d7cb2")

      val duration = TaskDuration(2, 0)
      val end = LocalDateTime.of(2021, 6, 20, 10, 3, 0)
      val timeDetails = TaskTimeDetails(start, end, duration)

      val givenTaskOption = Option(Task(projectId, authorId, timeDetails))

      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJwcm9qZWN0X2lkIjoicHJvamVjdF9pZCIsImF1dGhvcl9pZCI" +
        "6ImU1NGU1NjkyLTYwZDMtNGM4NC1hMjUxLTY2YWE5OThkN2NiMiIsInN0YXJ0X2RhdGUiOiIyMC0wNi0yMDIxIDA4OjAzIn0.hUoWoKx7D" +
        "GxDOA3I9Ub335wHiC6ZiR8z-gHBv1DTESM"

      val expectedJson =
        """
      {
        "success":false,
        "message":"Provided author is not author of the project",
        "data": ""
      }
      """

      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val taskRepository = mock[TaskRepository]
      val projectRepository = mock[ProjectRepository]

      val taskQuery = GetTaskByProjectIdAndStartQuery(projectId, start)

      when(taskRepository.find(taskQuery))
        .thenReturn(Future.successful(givenTaskOption))

      when(projectRepository.find(projectId, authorId))
        .thenReturn(Future.successful(Option.empty))

      val taskAggregate = new TaskAggregate(taskRepository)
      val projectAggregate = new ProjectAggregate(projectRepository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(DELETE)
        .withHeaders((AUTHORIZATION, bearer))

      When("Delete method is performed")
      val controller = new TaskController(Helpers.stubControllerComponents(), taskAggregate, projectAggregate, authHandler)
      val result: Future[Result] = controller.delete().apply(givenRequest)

      Then("Result should be with status Ok with expected json as body")
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.parse(expectedJson)
    }

    "be failed because of task to delete is already deleted" in {
      Given("Data needed to prepare request, expected result")
      val projectId = "project_id"
      val start = LocalDateTime.of(2021, 6, 20, 8, 3, 0)
      val authorId = UUID("e54e5692-60d3-4c84-a251-66aa998d7cb2")

      val duration = TaskDuration(2, 0)
      val end = LocalDateTime.of(2021, 6, 20, 10, 3, 0)
      val delete = LocalDateTime.of(2021, 6, 21, 12, 3, 0)
      val timeDetails = TaskTimeDetails(start, end, duration, Option(delete))

      val givenTaskOption = Option(Task(projectId, authorId, timeDetails))

      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJwcm9qZWN0X2lkIjoicHJvamVjdF9pZCIsImF1dGhvcl9pZCI" +
        "6ImU1NGU1NjkyLTYwZDMtNGM4NC1hMjUxLTY2YWE5OThkN2NiMiIsInN0YXJ0X2RhdGUiOiIyMC0wNi0yMDIxIDA4OjAzIn0.hUoWoKx7D" +
        "GxDOA3I9Ub335wHiC6ZiR8z-gHBv1DTESM"

      val expectedJson =
        """
      {
        "success":false,
        "message":"Task to delete is already deleted",
        "data": ""
      }
      """
      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val taskRepository = mock[TaskRepository]
      val projectRepository = mock[ProjectRepository]

      val taskQuery = GetTaskByProjectIdAndStartQuery(projectId, start)

      when(taskRepository.find(taskQuery))
        .thenReturn(Future.successful(givenTaskOption))

      val taskAggregate = new TaskAggregate(taskRepository)
      val projectAggregate = new ProjectAggregate(projectRepository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(DELETE)
        .withHeaders((AUTHORIZATION, bearer))

      When("Delete method is performed")
      val controller = new TaskController(Helpers.stubControllerComponents(), taskAggregate, projectAggregate, authHandler)
      val result: Future[Result] = controller.delete().apply(givenRequest)

      Then("Result should be with status Bad request with expected json as body")
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.parse(expectedJson)
    }
  }

  private def prepareGivenTask(projectId: String) = {
    val start = LocalDateTime.of(2021, 12, 23, 12, 0, 0)
    val duration = TaskDuration(2, 0)
    val end = LocalDateTime.of(2021, 12, 23, 14, 0, 0)
    val details = TaskTimeDetails(start, end, duration)
    val authorId = UUID("e54e5692-60d3-4c84-a251-66aa998d7cb1")
    Task(ObjectId.get(), projectId, authorId, details, Option.empty, Option.empty)
  }
}

package controllers

import akka.util.ByteString
import authentication.AuthenticationHandler
import common.StringUtils.EMPTY
import io.jvm.uuid.UUID
import org.bson.types.ObjectId
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
import play.api.test.Helpers.{AUTHORIZATION, DELETE, POST, PUT, contentAsJson, defaultAwaitTimeout, status}
import play.api.test.{FakeRequest, Helpers}
import project.{Project, ProjectAggregate, ProjectRepository}
import task._
import task.commands.{CreateTaskCommand, DeleteTaskCommand, UpdateTaskCommand}
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
      val bearer = EMPTY

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
      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
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
      when(projectRepository.find(projectId))
        .thenReturn(Future.successful(Option(Project(authorId, projectId))))
      when(taskRepository.create(CreateTaskCommand(projectId, authorId, timeDetails, Option(43), Option("Random comment"))))
        .thenReturn(Future.successful(response))

      val taskAggregate = new TaskAggregate(taskRepository)
      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
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
      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
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
      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
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
      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
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
      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
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
      when(projectRepository.find(projectId))
        .thenReturn(Future.successful(Option.empty))

      val taskAggregate = new TaskAggregate(taskRepository)
      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
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
        val projectId = "project_2"
        val start = LocalDateTime.of(2021, 12, 23, 13, 0, 0)
        val authorId = UUID("e54e5692-60d3-4c84-a251-66aa998d7cb2")

        val duration = TaskDuration(2, 32)
        val end = LocalDateTime.of(2021, 12, 23, 15, 32, 0)
        val timeDetails = TaskTimeDetails(start, end, duration)

        val givenTaskOption = Option(Task(projectId, authorId, timeDetails))

      val givenDeleteCommand = DeleteTaskCommand(projectId, authorId, start)

      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJwcm9qZWN0X2lkIjoicHJvamVjdF8yIiwiYXV0aG9yX2lkI" +
        "joiZTU0ZTU2OTItNjBkMy00Yzg0LWEyNTEtNjZhYTk5OGQ3Y2IyIiwic3RhcnRfZGF0ZSI6IjIzLTEyLTIwMjEgMTM6MDAiLCJkdXJhd" +
        "GlvbiI6IjIgaG91cnMgMzIgbWludXRlcyIsInZvbHVtZSI6NDMsImNvbW1lbnQiOiJSYW5kb20gY29tbWVudCJ9.03UbkBZgW9VT5LRQ" +
        "dtoZoSYVm7gTw-34zp-skI1IMZs"

      val givenBody =
        """
      {
        "success":true,
        "message":"Task created",
        "data": {
          "project_id":"unique_project_id_2",
          "author_id":"e54e5692-60d3-4c84-a251-66aa998d7cb1",
          "task_time_details": {
            "start_date":"23-12-2021 13:00",
            "duration": {
              "hours":2,
              "minutes":32
             }
          },
        "volume":43,
        "comment":"Random comment"}
      }
      """

      val expectedResult =
        """
      {
        "success":true,
        "message":"Task created",
        "data": {
          "project_id":"unique_project_id_2",
          "author_id":"e54e5692-60d3-4c84-a251-66aa998d7cb1",
          "task_time_details": {
            "start_date":"23-12-2021 13:00",
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

      val taskQuery = GetTaskByProjectIdAndStartQuery(projectId, start)

      val body = HttpEntity.Strict(ByteString(givenBody), Some(ContentTypes.JSON))
      val response = Result(ResponseHeader(OK), body)

      when(taskRepository.find(taskQuery))
        .thenReturn(Future.successful(givenTaskOption))

      when(projectRepository.find(projectId))
        .thenReturn(Future.successful(Option(Project(authorId, projectId))))

      when(taskRepository.deleteOne(givenDeleteCommand))
        .thenReturn(Future.successful(response))

      val taskAggregate = new TaskAggregate(taskRepository)
      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(DELETE)
        .withHeaders((AUTHORIZATION, bearer))

      When("Delete method is performed")
      val controller = new TaskController(Helpers.stubControllerComponents(), taskAggregate, projectAggregate, authHandler)
      val result: Future[Result] = controller.delete().apply(givenRequest)

      Then("Result should be with status Ok with expected json as body")
      //status(result) mustBe OK
      val value1 = contentAsJson(result)
      value1 mustBe Json.parse(expectedResult)
    }

    "be failed because of not valid author id" in {
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
      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
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
      val bearer = EMPTY

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
      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
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
      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
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
      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
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
      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
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
      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
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

      when(projectRepository.find(projectId))
        .thenReturn(Future.successful(Option(Project(UUID("f8015844-881d-46df-947a-f17179a769dc"), projectId))))

      val taskAggregate = new TaskAggregate(taskRepository)
      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
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
      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
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

  "TaskController#update" should {
    "be successful" in {
      Given("Data needed to prepare request, expected result")
      val projectIdOld = "project_2"
      val projectIdNew = "df"
      val authorIdOld = UUID("e54e5692-60d3-4c84-a251-66aa998d7cb2")
      val authorIdNew = UUID("5c2a9bf6-89fe-4328-b90e-df0d0c4aa77a")
      val startNew = LocalDateTime.of(2021, 12, 23, 15, 0, 0)
      val end = LocalDateTime.of(2021, 12, 23, 17, 32, 0)

      val duration = TaskDuration(2, 32)

      val timeDetails = TaskTimeDetails(startNew, end, duration)
      val givenUpdateCommand = UpdateTaskCommand(
        projectIdOld,
        projectIdNew,
        authorIdOld,
        authorIdNew,
        LocalDateTime.of(2021, 12, 23, 13, 0),
        timeDetails,
        Option(56),
        Option("Elo elo")
      )

      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJwcm9qZWN0X2lkX29sZCI6InByb2plY3RfMiIsInByb2plY3R" +
        "faWRfbmV3IjoiZGYiLCJhdXRob3JfaWRfb2xkIjoiZTU0ZTU2OTItNjBkMy00Yzg0LWEyNTEtNjZhYTk5OGQ3Y2IyIiwiYXV0aG9yX2lkX" +
        "25ldyI6IjVjMmE5YmY2LTg5ZmUtNDMyOC1iOTBlLWRmMGQwYzRhYTc3YSIsInN0YXJ0X2RhdGVfb2xkIjoiMjMtMTItMjAyMSAxMzowMC" +
        "IsInN0YXJ0X2RhdGVfbmV3IjoiMjMtMTItMjAyMSAxNTowMCIsImR1cmF0aW9uIjoiMiBob3VycyAzMiBtaW51dGVzIiwidm9sdW1lIjo1N" +
        "iwiY29tbWVudCI6IkVsbyBlbG8ifQ.evFwSEuaUAkvROnTQSc2MPUofo-ISy-p-drSAVYQRlI"

      val givenBody =
        """
      {
        "success":true,
        "message":"Task created",
        "data": {
          "project_id_old": "project_2",
          "project_id_new": "df",
          "author_id_old": "e54e5692-60d3-4c84-a251-66aa998d7cb2",
          "author_id_new": "5c2a9bf6-89fe-4328-b90e-df0d0c4aa77a",
          "start_date_old": "23-12-2021 13:00",
          "start_date_new": "23-12-2021 15:00",
          "duration": {
             "hours": 2,
             "minutes": 32
          },
          "volume": 56,
          "comment": "Elo elo"
        }
      }
      """

      val expectedResult =
        """
      {
        "success":true,
        "message":"Task created",
        "data": {
          "project_id_old":"project_2",
          "project_id_new": "df",
          "author_id_old": "e54e5692-60d3-4c84-a251-66aa998d7cb2",
          "author_id_new": "5c2a9bf6-89fe-4328-b90e-df0d0c4aa77a",
          "start_date_old": "23-12-2021 13:00",
          "start_date_new": "23-12-2021 15:00",
          "duration": {
             "hours": 2,
             "minutes": 32
          },
          "volume": 56,
          "comment": "Elo elo"
        }
      }
      """

      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val taskRepository = mock[TaskRepository]
      val projectRepository = mock[ProjectRepository]

      val taskQuery = GetTaskByProjectIdAndTimeDetailsQuery(projectIdNew, timeDetails)

      val body = HttpEntity.Strict(ByteString(givenBody), Some(ContentTypes.JSON))
      val response = Result(ResponseHeader(OK), body)

      when(taskRepository.find(taskQuery))
        .thenReturn(Future.successful(Option.empty))

      when(projectRepository.find(projectIdOld/*, authorIdOld*/))
        .thenReturn(Future.successful(Option(Project(authorIdOld, projectIdOld))))

      when(taskRepository.update(givenUpdateCommand))
        .thenReturn(Future.successful(response))

      val taskAggregate = new TaskAggregate(taskRepository)
      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(PUT)
        .withHeaders((AUTHORIZATION, bearer))

      When("Update method is performed")
      val controller = new TaskController(Helpers.stubControllerComponents(), taskAggregate, projectAggregate, authHandler)
      val result: Future[Result] = controller.update().apply(givenRequest)

      Then("Result should be with status Ok with expected json as body")
      status(result) mustBe OK
      contentAsJson(result) mustBe Json.parse(expectedResult)
    }

    "be failed because of incorrect Bearer token" in {
      Given("Data needed to prepare request, expected result")
      val bearer = EMPTY

      val expectedResult =
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
      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(PUT)
        .withHeaders((AUTHORIZATION, bearer))

      When("Update method is performed")
      val controller = new TaskController(Helpers.stubControllerComponents(), taskAggregate, projectAggregate, authHandler)
      val result: Future[Result] = controller.update().apply(givenRequest)

      Then("Result should be with status Bad request with expected json as body")
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.parse(expectedResult)
    }

    "be failed, because of new project id is empty" in {
      Given("Data needed to prepare request, expected result")

      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJwcm9qZWN0X2lkX25ldyI6ImRmIiwiYXV0aG9yX2lkX29sZCI6" +
        "ImU1NGU1NjkyLTYwZDMtNGM4NC1hMjUxLTY2YWE5OThkN2NiMiIsImF1dGhvcl9pZF9uZXciOiJlNTRlNTY5Mi02MGQzLTRjODQtYTI1MS0" +
        "2NmFhOTk4ZDdjYjEiLCJzdGFydF9kYXRlX29sZCI6IjIzLTEyLTIwMjEgMTM6MDAiLCJzdGFydF9kYXRlX25ldyI6IjIzLTEyLTIwMjEgMT" +
        "U6MzIiLCJkdXJhdGlvbiI6IjIgaG91cnMgMzIgbWludXRlcyIsInZvbHVtZSI6MTEsImNvbW1lbnQiOiJTaWVtYSBzaWVtYSJ9.gw_EVT2W" +
        "lbvHZ8hX-HveYtxea3XbI3HEvdmPmwtRiYs"

      val expectedResult =
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
      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(PUT)
        .withHeaders((AUTHORIZATION, bearer))

      When("Update method is performed")
      val controller = new TaskController(Helpers.stubControllerComponents(), taskAggregate, projectAggregate, authHandler)
      val result: Future[Result] = controller.update().apply(givenRequest)

      Then("Result should be with status Bad request with expected json as body")
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.parse(expectedResult)
    }

    "be failed because of empty author id" in {
      Given("Data needed to prepare request, expected result")

      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJwcm9qZWN0X2lkX29sZCI6InByb2plY3RfMiIsInByb" +
        "2plY3RfaWRfbmV3IjoiZGYiLCJhdXRob3JfaWRfb2xkIjoiZTU0ZTU2OTItNjBkMy00Yzg0LWEyNTEtNjZhYTk5OGQ3Y2IyIiwic" +
        "3RhcnRfZGF0ZV9vbGQiOiIyMy0xMi0yMDIxIDEzOjAwIiwic3RhcnRfZGF0ZV9uZXciOiIyMy0xMi0yMDIxIDE1OjMyIiwiZHVyY" +
        "XRpb24iOiIyIGhvdXJzIDMyIG1pbnV0ZXMiLCJ2b2x1bWUiOjExLCJjb21tZW50IjoiU2llbWEgc2llbWEifQ.1h5HOJwKt6JzLw" +
        "xaTGKFmGm1-bKz6jtl-LZC-p6jln0"

      val expectedResult =
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
      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(PUT)
        .withHeaders((AUTHORIZATION, bearer))

      When("Update method is performed")
      val controller = new TaskController(Helpers.stubControllerComponents(), taskAggregate, projectAggregate, authHandler)
      val result: Future[Result] = controller.update().apply(givenRequest)

      Then("Result should be with status Bad request with expected json as body")
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.parse(expectedResult)
    }

    "be failed because of incorrect start date" in {
      Given("Data needed to prepare request, expected result")

      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJwcm9qZWN0X2lkX29sZCI6InByb2plY3RfMiIsInByb2plY3Rf" +
        "aWRfbmV3IjoiZGYiLCJhdXRob3JfaWRfb2xkIjoiZTU0ZTU2OTItNjBkMy00Yzg0LWEyNTEtNjZhYTk5OGQ3Y2IyIiwic3RhcnRfZGF0ZV9" +
        "vbGQiOiIyMy0xMi0yMDIxIDEzOjAwIiwiYXV0aG9yX2lkX25ldyI6IjVjMmE5YmY2LTg5ZmUtNDMyOC1iOTBlLWRmMGQwYzRhYTc3YSIsIm" +
        "R1cmF0aW9uIjoiMiBob3VycyAzMiBtaW51dGVzIiwidm9sdW1lIjoxMSwiY29tbWVudCI6IlNpZW1hIHNpZW1hIn0.p9cDTrjcno39hA52Q" +
        "KBR3C8jdd7HjRVoqNm7ixl7zVk"

      val expectedResult =
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
      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(PUT)
        .withHeaders((AUTHORIZATION, bearer))

      When("Update method is performed")
      val controller = new TaskController(Helpers.stubControllerComponents(), taskAggregate, projectAggregate, authHandler)
      val result: Future[Result] = controller.update().apply(givenRequest)

      Then("Result should be with status Bad request with expected json as body")
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.parse(expectedResult)
    }

    "be failed because of incorrect duration" in {
      Given("Data needed to prepare request, expected result")

      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJwcm9qZWN0X2lkX29sZCI6InByb2plY3RfMiIsInByb2plY3" +
        "RfaWRfbmV3IjoiZGYiLCJhdXRob3JfaWRfb2xkIjoiZTU0ZTU2OTItNjBkMy00Yzg0LWEyNTEtNjZhYTk5OGQ3Y2IyIiwiYXV0aG9yX2l" +
        "kX25ldyI6IjVjMmE5YmY2LTg5ZmUtNDMyOC1iOTBlLWRmMGQwYzRhYTc3YSIsInN0YXJ0X2RhdGVfbmV3IjoiMjMtMTItMjAyMSAxNTow" +
        "MCIsInN0YXJ0X2RhdGVfb2xkIjoiMjMtMTItMjAyMSAxMzowMCIsImR1cmF0aW9uIjoic2ZrbmdkaGpmZGZqIiwidm9sdW1lIjoxMSwiY" +
        "29tbWVudCI6IlNpZW1hIHNpZW1hIn0.c1T4bLfKmB4HlUFe_ojIxx1zdwperqKaAPNRFFqs78s"

      val expectedResult =
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
      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(PUT)
        .withHeaders((AUTHORIZATION, bearer))

      When("Update method is performed")
      val controller = new TaskController(Helpers.stubControllerComponents(), taskAggregate, projectAggregate, authHandler)
      val result: Future[Result] = controller.update().apply(givenRequest)

      Then("Result should be with status Bad request with expected json as body")
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.parse(expectedResult)
    }

    "be failed because of task in conflict with another task" in {
      Given("Data needed to prepare request, expected result")
      val projectIdOld = "project_2"
      val projectIdNew = "df"
      val authorIdOld = UUID("e54e5692-60d3-4c84-a251-66aa998d7cb2")
      val startNew = LocalDateTime.of(2021, 12, 23, 15, 0, 0)
      val end = LocalDateTime.of(2021, 12, 23, 17, 32, 0)

      val duration = TaskDuration(2, 32)

      val timeDetails = TaskTimeDetails(startNew, end, duration)

      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJwcm9qZWN0X2lkX29sZCI6InByb2plY3RfMiIsInByb2plY3R" +
        "faWRfbmV3IjoiZGYiLCJhdXRob3JfaWRfb2xkIjoiZTU0ZTU2OTItNjBkMy00Yzg0LWEyNTEtNjZhYTk5OGQ3Y2IyIiwiYXV0aG9yX2lkX" +
        "25ldyI6IjVjMmE5YmY2LTg5ZmUtNDMyOC1iOTBlLWRmMGQwYzRhYTc3YSIsInN0YXJ0X2RhdGVfb2xkIjoiMjMtMTItMjAyMSAxMzowMC" +
        "IsInN0YXJ0X2RhdGVfbmV3IjoiMjMtMTItMjAyMSAxNTowMCIsImR1cmF0aW9uIjoiMiBob3VycyAzMiBtaW51dGVzIiwidm9sdW1lIjo1N" +
        "iwiY29tbWVudCI6IkVsbyBlbG8ifQ.evFwSEuaUAkvROnTQSc2MPUofo-ISy-p-drSAVYQRlI"

      val expectedResult =
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

      val taskQuery = GetTaskByProjectIdAndTimeDetailsQuery(projectIdNew, timeDetails)

      when(taskRepository.find(taskQuery))
        .thenReturn(Future.successful(Option(Task(projectIdOld, authorIdOld, timeDetails))))

      val taskAggregate = new TaskAggregate(taskRepository)
      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(PUT)
        .withHeaders((AUTHORIZATION, bearer))

      When("Update method is performed")
      val controller = new TaskController(Helpers.stubControllerComponents(), taskAggregate, projectAggregate, authHandler)
      val result: Future[Result] = controller.update().apply(givenRequest)

      Then("Result should be with status Bad request with expected json as body")
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.parse(expectedResult)
    }

    "be failed because of not existing project" in {
      Given("Data needed to prepare request, expected result")

      val projectIdNew = "df"
      val projectIdOld = "project_2"
      val startNew = LocalDateTime.of(2021, 12, 23, 15, 0, 0)
      val end = LocalDateTime.of(2021, 12, 23, 17, 32, 0)

      val duration = TaskDuration(2, 32)

      val timeDetails = TaskTimeDetails(startNew, end, duration)

      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJwcm9qZWN0X2lkX29sZCI6InByb2plY3RfMiIsInByb2plY3R" +
        "faWRfbmV3IjoiZGYiLCJhdXRob3JfaWRfb2xkIjoiZTU0ZTU2OTItNjBkMy00Yzg0LWEyNTEtNjZhYTk5OGQ3Y2IyIiwiYXV0aG9yX2lkX" +
        "25ldyI6IjVjMmE5YmY2LTg5ZmUtNDMyOC1iOTBlLWRmMGQwYzRhYTc3YSIsInN0YXJ0X2RhdGVfb2xkIjoiMjMtMTItMjAyMSAxMzowMC" +
        "IsInN0YXJ0X2RhdGVfbmV3IjoiMjMtMTItMjAyMSAxNTowMCIsImR1cmF0aW9uIjoiMiBob3VycyAzMiBtaW51dGVzIiwidm9sdW1lIjo1N" +
        "iwiY29tbWVudCI6IkVsbyBlbG8ifQ.evFwSEuaUAkvROnTQSc2MPUofo-ISy-p-drSAVYQRlI"

      val expectedResult =
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

      val taskQuery = GetTaskByProjectIdAndTimeDetailsQuery(projectIdNew, timeDetails)

      when(taskRepository.find(taskQuery))
        .thenReturn(Future.successful(Option.empty))

      when(projectRepository.find(projectIdOld))
        .thenReturn(Future.successful(Option.empty))

      val taskAggregate = new TaskAggregate(taskRepository)
      val projectAggregate = new ProjectAggregate(projectRepository, taskRepository)
      val authHandler = AuthenticationHandler(config)
      val givenRequest = FakeRequest()
        .withMethod(PUT)
        .withHeaders((AUTHORIZATION, bearer))

      When("Update method is performed")
      val controller = new TaskController(Helpers.stubControllerComponents(), taskAggregate, projectAggregate, authHandler)
      val result: Future[Result] = controller.update().apply(givenRequest)

      Then("Result should be with status Bad request with expected json as body")
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.parse(expectedResult)
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

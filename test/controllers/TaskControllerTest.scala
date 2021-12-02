package controllers

import akka.util.ByteString
import authentication.AuthenticationHandler
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
import play.api.test.Helpers.{AUTHORIZATION, POST, contentAsJson, defaultAwaitTimeout, status}
import play.api.test.{FakeRequest, Helpers}
import project.{Project, ProjectAggregate, ProjectRepository}
import task.commands.CreateTaskCommand
import task.queries.GetTaskByProjectIdAndTimeDetailsQuery
import task.{Task, TaskAggregate, TaskDuration, TaskRepository, TaskTimeDetails}

import java.time.LocalDateTime
import scala.concurrent.Future

class TaskControllerTest extends PlaySpec with MockitoSugar with GivenWhenThen with ScalaFutures {
  private implicit val defaultPatience: PatienceConfig = {
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))
  }

  "TaskController#create" should {
    "be successful" in {
      Given("Data needed to prepare request, expected result")
      val projectId = "unique_project_id_2"
      val start = LocalDateTime.of(2021, 12, 23, 13, 0, 0)
      val duration = TaskDuration(2, 32)
      val end = LocalDateTime.of(2021, 12, 23, 15, 32, 0)
      val timeDetails = TaskTimeDetails(start, end, duration)

      val bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJwcm9qZWN0X2lkIjoidW5pcXVlX3Byb2plY3RfaWRfMiIsInN0Y" +
        "XJ0X2RhdGUiOiIyMy0xMi0yMDIxIDEzOjAwIiwiZHVyYXRpb24iOiIyIGhvdXJzIDMyIG1pbnV0ZXMiLCJ2b2x1bWUiOjQzLCJjb21tZW50I" +
        "joiUmFuZG9tIGNvbW1lbnQifQ.VLBZBiTAMu-3WMEu6rSL0MpshGpuSVwSircoIs1iTqM"

      val expectedJson =
        """
      {
        "success":true,
        "message":"Task created",
        "data": {
          "project_id":"unique_project_id_2",
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
        .thenReturn(Future.successful(Option(Project(UUID("e54e5692-60d3-4c84-a251-66aa998d7cb1"), projectId))))
      when(taskRepository.create(CreateTaskCommand(projectId, timeDetails, Option(43), Option("Random comment"))))
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
  }

  "TaskController#create" should {
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
  }

  "TaskController#create" should {
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
  }

  "TaskController#create" should {
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
  }

  "TaskController#create" should {
    "returns error response because of existing time in conflict" in {
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
  }

  private def prepareGivenTask(projectId: String) = {
    val start = LocalDateTime.of(2021, 12, 23, 12, 0, 0)
    val duration = TaskDuration(2, 0)
    val end = LocalDateTime.of(2021, 12, 23, 14, 0, 0)
    val details = TaskTimeDetails(start, end, duration)
    Task(ObjectId.get(), projectId, details, Option.empty, Option.empty)
  }
}

package project

import io.jvm.uuid.UUID
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.GivenWhenThen
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Results.{Created, InternalServerError}
import project.commands.CreateProjectCommand

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ProjectAggregateTest extends PlaySpec with MockitoSugar with GivenWhenThen with ScalaFutures {
  private implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))
  "ProjectAggregate#createProject" should {
    "be valid" in {
      Given("Project repository, create project command")

      val uuid = UUID("57f53578-a94a-4f9c-b8c2-8e89b830ae61")
      val projectId = "test_project_id"
      val command = CreateProjectCommand(uuid, projectId)
      val projectRepository = mock[ProjectRepository]
      when(projectRepository.create(new Project(any, uuid, projectId))).thenReturn(Future(Created("Project created")))

      When("is performed ProjectAggregate#createProject")
      val result = new ProjectAggregate(projectRepository).createProject(command)

      Then("Result should be Created with message: \"Project created\"")
      whenReady(result) { value => value mustBe Created("Project created")}
    }
  }

  "ProjectAggregate#createProject" should {
    "be failed" in {
      Given("Project repository, create project command")

      val uuid = UUID("57f53578-a94a-4f9c-b8c2-8e89b830ae61")
      val projectId = "test_project_id"
      val command = CreateProjectCommand(uuid, projectId)
      val projectRepository = mock[ProjectRepository]
      when(projectRepository.create(new Project(any, uuid, projectId))).thenReturn(Future(InternalServerError("Database error")))

      When("is performed ProjectAggregate#createProject")
      val result = new ProjectAggregate(projectRepository).createProject(command)

      Then("Result should be InternalServerError with message: \"Database error\"")
      whenReady(result) { value => value mustBe InternalServerError("Database error")}
    }
  }
}
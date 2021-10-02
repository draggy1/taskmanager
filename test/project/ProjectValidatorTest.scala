package project

import authentication.{DuplicatedProjectId, EmptyProjectId, EmptyUserId}
import common.StringUtils.EMPTY
import io.jvm.uuid.UUID
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{EitherValues, GivenWhenThen}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import project.commands.CreateProjectCommand
import project.queries.GetProjectByIdQuery

import scala.concurrent.Future

class ProjectValidatorTest extends PlaySpec with MockitoSugar with GivenWhenThen with ScalaFutures with EitherValues {
  "Project validator" should {
    "has successful result" in {
      Given("UUID, id of project, command and aggregate")
      val uuid = UUID("11e2c7f8-1244-5d23-b6c0-86f1b4f565ed")
      val projectId = "test_project_id"
      val command = CreateProjectCommand(uuid, projectId)
      val aggregate = mock[ProjectAggregate]
      when(aggregate.getProject(GetProjectByIdQuery(command.projectId)))
        .thenReturn(Future.successful(Option.empty))

      When("validation is performed")

      val validator = ProjectValidator(aggregate)
      val result = validator.validate(command)

      Then("result is the command which will be needed to create project")
      whenReady(result) { value => value mustBe Right(command) }
    }
  }

  "Project validator" should {
    "returns duplicated project error" in {
      Given("UUID, id of project, command and aggregate")
      val uuid = UUID("11e2c7f8-1244-5d23-b6c0-86f1b4f565ed")
      val projectId = "test_project_id"
      val command = CreateProjectCommand(uuid, projectId)
      val aggregate = mock[ProjectAggregate]
      when(aggregate.getProject(GetProjectByIdQuery(command.projectId)))
        .thenReturn(Future.successful(Option(Project(uuid, projectId))))

      When("validation is performed")

      val validator = ProjectValidator(aggregate)
      val result = validator.validate(command)

      Then("result is duplicated project error")
      whenReady(result) { value => value.left.value mustBe DuplicatedProjectId }
    }
  }

  "Project validator" should {
    "returns empty user id error" in {
      Given("UUID, id of project, command and aggregate")
      val uuid = UUID("00000000-0000-0000-0000-000000000000")
      val projectId = "test_project_id"
      val command = CreateProjectCommand(uuid, projectId)
      val aggregate = mock[ProjectAggregate]
      when(aggregate.getProject(GetProjectByIdQuery(command.projectId)))
        .thenReturn(Future.successful(Option(Project(uuid, projectId))))

      When("validation is performed")

      val validator = ProjectValidator(aggregate)
      val result = validator.validate(command)

      Then("result is empty user id error")
      whenReady(result) { value => value.left.value mustBe EmptyUserId }
    }
  }

  "Project validator" should {
    "returns empty project id error" in {
      Given("UUID, id of project, command and aggregate")
      val uuid = UUID("11e2c7f8-1244-5d23-b6c0-86f1b4f565ed")
      val command = CreateProjectCommand(uuid, EMPTY)
      val aggregate = mock[ProjectAggregate]
      when(aggregate.getProject(GetProjectByIdQuery(command.projectId)))
        .thenReturn(Future.successful(Option.empty))

      When("validation is performed")

      val validator = ProjectValidator(aggregate)
      val result = validator.validate(command)

      Then("result is empty project id error")
      whenReady(result) { value => value.left.value mustBe EmptyProjectId }
    }
  }
}

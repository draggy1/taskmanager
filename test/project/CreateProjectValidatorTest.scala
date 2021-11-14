/*
package project

import authentication.{DuplicatedProjectId, EmptyProjectId, EmptyAuthorId}
import common.StringUtils.EMPTY
import io.jvm.uuid.UUID
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{EitherValues, GivenWhenThen}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import project.commands.CreateProjectCommand
import project.queries.GetProjectByIdQuery
import project.validators.CreateProjectValidator

import java.time.LocalDateTime
import scala.concurrent.Future

class CreateProjectValidatorTest extends PlaySpec with MockitoSugar with GivenWhenThen with ScalaFutures with EitherValues {
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

      val validator = CreateProjectValidator(aggregate)
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
      val timestamp = LocalDateTime.of(2021, 12, 13, 0,0)
      val command = CreateProjectCommand(uuid, projectId)
      val aggregate = mock[ProjectAggregate]
      when(aggregate.getProject(GetProjectByIdQuery(command.projectId)))
        .thenReturn(Future.successful(Option(Project(uuid, projectId, timestamp))))

      When("validation is performed")

      val validator = CreateProjectValidator(aggregate)
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
      val timestamp = LocalDateTime.of(2021, 12, 13, 0,0)
      when(aggregate.getProject(GetProjectByIdQuery(command.projectId)))
        .thenReturn(Future.successful(Option(Project(uuid, projectId, timestamp))))

      When("validation is performed")

      val validator = CreateProjectValidator(aggregate)
      val result = validator.validate(command)

      Then("result is empty user id error")
      whenReady(result) { value => value.left.value mustBe EmptyAuthorId }
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

      val result = CreateProjectValidator(aggregate).validate(command)

      Then("result is empty project id error")
      whenReady(result) { value => value.left.value mustBe EmptyProjectId }
    }
  }
}
*/

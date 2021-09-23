package project

import authentication.DuplicatedProjectId
import io.jvm.uuid.UUID
import org.mockito.Mockito.when
import org.scalatest.{EitherValues, GivenWhenThen}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import project.commands.CreateProjectCommand
import project.queries.GetProjectByIdQuery

import scala.concurrent.Future

class ProjectValidatorTest extends PlaySpec with MockitoSugar with GivenWhenThen with ScalaFutures with EitherValues {
  "Project validator" should {
    "returns error" in {
      Given("UUID and id of project, command and aggregate")
      val uuid = UUID("57f53578-a94a-4f9c-b8c2-8e89b830ae61")
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
    "has successful result" in {
      Given("UUID and id of project, command and aggregate")
      val uuid = UUID("57f53578-a94a-4f9c-b8c2-8e89b830ae61")
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
}

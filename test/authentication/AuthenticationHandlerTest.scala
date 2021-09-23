package authentication

import org.mockito.Mockito.when
import org.scalatest.{EitherValues, GivenWhenThen}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import pdi.jwt.JwtClaim
import play.api.Configuration
import play.api.test.FakeRequest
import play.api.test.Helpers.{AUTHORIZATION, POST}

import scala.language.postfixOps

class AuthenticationHandlerTest extends PlaySpec with MockitoSugar with GivenWhenThen with EitherValues {
  "Authentication" should {
    "be performed successfully" in {
      Given("configuration which returns jwt secret key and prepared request")
      val config = mock[Configuration]
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      val givenRequest = FakeRequest()
        .withMethod(POST)
        .withHeaders((AUTHORIZATION, "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiZTU0ZTU2OTItNjBkMy00Yzg0LWEyNTEtNjZhYTk5OGQ3Y2IxIiwicHJvamVjdF9pZCI6InVuaXF1ZV9wcm9qZWN0X2lkXzEifQ.PNDMAcOVUQXLaVR1Tp2wyAQhNUOBi7Luq5MOrlINJTg"))

      When("authentication is performed")
      val result = AuthenticationHandler(config).performWithAuthentication(givenRequest)

      Then("result is proper Jwt claim")
      val expected = Right(JwtClaim("{\"user_id\":\"e54e5692-60d3-4c84-a251-66aa998d7cb1\",\"project_id\":\"unique_project_id_1\"}", None, None, None, None, None, None, None))
      result mustBe expected
    }
  }

  "Authentication" should {
    "be failed on request without authorization header" in {
      Given("configuration which returns jwt secret key and prepared request without authorization header")
      val config = mock[Configuration]
      val givenRequest = FakeRequest().withMethod(POST)
      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      When("authentication is performed")
      val result = AuthenticationHandler(config).performWithAuthentication(givenRequest)

      Then("result is \"Request not contains authorization header\"")
      val left: Error  = result.left.value
      left mustBe WithoutHeader
    }
  }

  "Authentication" should {
    "be failed on request with \"Basic\" authorization header" in {
      Given("configuration which returns jwt secret key and prepared request with \"Basic\" authorization header")
      val config = mock[Configuration]
      val givenRequest = FakeRequest()
        .withMethod(POST)
        .withHeaders((AUTHORIZATION, "Basic YWxhZGRpbjpvcGVuc2VzYW1l"))

      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      When("authentication is performed")
      val result = AuthenticationHandler(config).performWithAuthentication(givenRequest)

      Then("result is \"Request not contains authorization header\"")
      val left: Error  = result.left.value
      left mustBe WithoutBearerToken
    }
  }

  "Authentication" should {
    "be failed on request with incorrect JWT token" in {
      Given("configuration which returns jwt secret key and prepared request with incorrect JWT token")
      val config = mock[Configuration]
      val givenRequest = FakeRequest()
        .withMethod(POST)
        .withHeaders((AUTHORIZATION, "Bearer IncorrectToken"))

      when(config.get[String]("secret.key")).thenReturn("Test secret key")

      When("authentication is performed")
      val result = AuthenticationHandler(config).performWithAuthentication(givenRequest)

      Then("result is \"Could not decode JWT token\"")
      val left: Error  = result.left.value
      left mustBe IncorrectJwtToken
    }
  }
}

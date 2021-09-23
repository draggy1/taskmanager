package it

import configuration.MongoDbManager
import controllers.ProjectController
import org.scalatest.TestData
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerTest
import play.api.Application
import play.api.http.Status.OK
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind
import play.api.libs.ws.WSClient
import play.api.test.Helpers.await

class ProjectIt extends PlaySpec with GuiceOneServerPerTest {

  // Override newAppForTest or mixin GuiceFakeApplicationFactory and use fakeApplication() for an Application
  override def newAppForTest(testData: TestData): Application = {
    GuiceApplicationBuilder()
      .appRoutes(app => {
        case ("GET", "/") => app.injector.instanceOf(classOf[ProjectController]).createProject()
      })
      .bindings(bind[MongoDbManager].to[MongoDbManagerTest])
      .build()
  }

  "The OneServerPerTest trait" must {
    "test server logic" in {
      val wsClient              = app.injector.instanceOf[WSClient]
      val myPublicAddress       = s"localhost:$port"
      val testPaymentGatewayURL = s"http://$myPublicAddress"
      // The test payment gateway requires a callback to this server before it returns a result...
      val callbackURL = s"http://$myPublicAddress/callback"
      // await is from play.api.test.FutureAwaits
      val response =
        await(wsClient.url(testPaymentGatewayURL).addQueryStringParameters("callbackURL" -> callbackURL).get())

      response.status mustBe OK
    }
  }
}

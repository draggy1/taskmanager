package common

import akka.util.ByteString
import play.api.http.{ContentTypes, HttpEntity}
import play.api.libs.json.{JsBoolean, JsObject, JsString, JsValue, Json, Writes}
import play.api.mvc.{ResponseHeader, Result}

case object Response {
  implicit def responseWrites[D](implicit fmt: Writes[D]): Writes[Response[D]] = (ts: Response[D]) => JsObject(Seq(
    "success" -> JsBoolean(ts.success),
    "message" -> JsString(ts.message),
    "data" -> Json.toJson(ts.data)
  ))

  def getResult(header: ResponseHeader, json: JsValue): Result = {
    val body = HttpEntity.Strict(ByteString(json.toString()), Some(ContentTypes.JSON))
    Result(header, body)
  }
}

case class Response[D](success: Boolean, message: String, data: D)


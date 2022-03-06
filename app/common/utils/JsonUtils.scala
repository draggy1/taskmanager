package common.utils

import common.Command
import common.responses.Response
import play.api.libs.json.{JsValue, Json, Writes}

object JsonUtils {
    def prepareSuccessJson[C <: Command](message: String, command: C)(implicit writes: Writes[C]): JsValue =
        Json.toJson(Response[C](success = true, message, command))
}

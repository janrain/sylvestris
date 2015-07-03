package sylvestris.core

import spray.json._

case class Error(message: String, throwable: Option[Throwable] = None)

object Error {

  implicit object jsonWriter extends RootJsonWriter[Error] {
    def write(e: Error) = JsObject("message" -> JsString(e.message))
  }

}

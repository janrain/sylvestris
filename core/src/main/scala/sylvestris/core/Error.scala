package sylvestris.core

import algebra.Eq
import spray.json._

case class Error(message: String, throwable: Option[Throwable])

object Error {

  def apply(message: String): Error = Error(message, None)

  implicit object jsonFormatter extends RootJsonFormat[Error] {
    def write(e: Error) = JsObject("message" -> JsString(e.message))
    def read(v: JsValue) = ???
  }

  implicit val eqInstance: Eq[Error] = Eq.fromUniversalEquals[Error]
}

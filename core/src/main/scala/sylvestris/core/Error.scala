package sylvestris.core

import spray.json._, DefaultJsonProtocol._

case class Error(message: String)

object Error {
  implicit val jsonFormat = jsonFormat1(apply)
}

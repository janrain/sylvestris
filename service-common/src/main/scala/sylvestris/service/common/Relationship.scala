package sylvestris.service.common

import spray.json._, DefaultJsonProtocol._

case class Relationship(nodePath: String)

object Relationship {
  implicit val jsonFormat = jsonFormat1(apply)
}

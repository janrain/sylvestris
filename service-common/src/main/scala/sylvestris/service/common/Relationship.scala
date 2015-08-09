package sylvestris.service.common

import spray.json._, DefaultJsonProtocol._

case class Relationship(label: Option[String], nodePath: String)

object Relationship {
  implicit val jsonFormat = jsonFormat2(apply)
}

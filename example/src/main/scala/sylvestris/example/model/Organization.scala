package sylvestris.example.model

import sylvestris.core._
import spray.json._, DefaultJsonProtocol._

object Organization {
  implicit object nodeManifest extends NodeManifest[Organization] {
    implicit val tag = Tag("org")
    implicit val jsonFormat = jsonFormat1(apply)
  }
}

case class Organization(name: String)

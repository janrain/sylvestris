package sylvestris.example.model

import spray.json._, DefaultJsonProtocol._
import sylvestris.core._

object Organization {
  implicit val nodeManifest = NodeManifest[Organization](Tag("org"), jsonFormat1(apply))
}

case class Organization(name: String)

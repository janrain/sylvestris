package sylvestris.example.model

import spray.json._, DefaultJsonProtocol._
import sylvestris.core._

object Customer {
  implicit val nodeManifest = NodeManifest[Customer](Tag("cust"), jsonFormat1(apply))
}

case class Customer(name: String)

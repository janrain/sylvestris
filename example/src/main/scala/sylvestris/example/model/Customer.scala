package sylvestris.example.model

import sylvestris.core._
import spray.json._, DefaultJsonProtocol._

object Customer {
  implicit object nodeManifest extends NodeManifest[Customer] {
    implicit val tag = Tag("cust")
    implicit val jsonFormat = jsonFormat1(apply)
  }
}

case class Customer(name: String)

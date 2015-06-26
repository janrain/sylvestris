package model

import graph._
import spray.json._, DefaultJsonProtocol._

object Customer {
  implicit object nodeManifest extends NodeManifest[Customer] {
    implicit val tag = Tag("cust")
    implicit val jsonFormat = jsonFormat1(apply)
    implicit object validation extends Validation[Customer]
  }
}

case class Customer(name: String)

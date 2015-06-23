package model

import graph._
import spray.json._, DefaultJsonProtocol._

object Customer {
  implicit object nodeManifest extends NodeManifest[Customer] {
    implicit object tag extends Tag[Customer]("cust")
    implicit val jsonFormat = jsonFormat1(apply)
    implicit object validation extends Validation[Customer]
  }
}

case class Customer(name: String)

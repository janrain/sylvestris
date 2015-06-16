package model

import graph._
import spray.json._, DefaultJsonProtocol._

object Customer {
  implicit object tag extends Tag[Customer]("cust")
  implicit val jsonFormat = jsonFormat1(apply)
}

case class Customer(name: String)

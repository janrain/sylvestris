package model

import graph._
import service._
import spray.json._, DefaultJsonProtocol._

object Organization {
  implicit object tag extends Tag[Organization]("org")
  implicit object pathSegment extends PathSegment[Organization]("orgs")
  implicit val jsonFormat = jsonFormat1(apply)
}

case class Organization(name: String)

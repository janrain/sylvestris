package model

import graph._
import spray.json._, DefaultJsonProtocol._

object Organization {
  implicit object nodeManifest extends NodeManifest[Organization] {
    implicit val tag = Tag("org")
    implicit val jsonFormat = jsonFormat1(apply)
    implicit object validation extends Validation[Organization]
  }
}

case class Organization(name: String)

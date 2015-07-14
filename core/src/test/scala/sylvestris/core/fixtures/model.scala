package sylvestris.core.fixtures

import spray.json._, DefaultJsonProtocol._
import sylvestris.core._

object model {
  object Content1 {
    implicit object nodeManifest extends NodeManifest[Content1] {
      implicit val tag = Tag("content")
      implicit val jsonFormat = jsonFormat1(apply)
    }
  }
  case class Content1(v: String)

  object Content2 {
    implicit object nodeManifest extends NodeManifest[Content2] {
      implicit val tag = Tag("content2")
      implicit val jsonFormat = jsonFormat1(apply)
    }
  }
  case class Content2(v: String)
}

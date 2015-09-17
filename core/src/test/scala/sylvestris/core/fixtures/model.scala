package sylvestris.core.fixtures

import spray.json._, DefaultJsonProtocol._
import sylvestris.core._

// TODO : investigate
@SuppressWarnings(Array(
  "org.brianmckenna.wartremover.warts.AsInstanceOf",
  "org.brianmckenna.wartremover.warts.Throw"))
object model {
  object Content1 {
    implicit val nodeManifest = NodeManifest[Content1](Tag("content"), jsonFormat1(apply))
  }
  case class Content1(v: String)

  object Content2 {
    implicit val nodeManifest = NodeManifest[Content2](Tag("content2"), jsonFormat1(apply))
  }
  case class Content2(v: String)
}

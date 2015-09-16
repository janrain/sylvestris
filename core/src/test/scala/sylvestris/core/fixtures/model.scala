package sylvestris.core.fixtures

import fommil.sjs.FamilyFormats._
import sylvestris.core._

object model {
  object Content1 {
    implicit val nodeManifest = NodeManifest[Content1](Tag("content"))
  }
  case class Content1(v: String)

  object Content2 {
    implicit val nodeManifest = NodeManifest[Content2](Tag("content2"))
  }
  case class Content2(v: String)
}

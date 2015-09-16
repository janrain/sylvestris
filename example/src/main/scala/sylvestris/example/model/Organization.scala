package sylvestris.example.model

import fommil.sjs.FamilyFormats._
import sylvestris.core._

object Organization {
  implicit val nodeManifest = NodeManifest[Organization](Tag("org"))
}

case class Organization(name: String)

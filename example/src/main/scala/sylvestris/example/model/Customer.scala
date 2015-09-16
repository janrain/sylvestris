package sylvestris.example.model

import fommil.sjs.FamilyFormats._
import sylvestris.core._

object Customer {
  implicit val nodeManifest = NodeManifest[Customer](Tag("cust"))
}

case class Customer(name: String)

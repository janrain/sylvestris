package model

import graph._

object relationships {
  implicit object orgCustomer extends OneToOne[Organization, Customer]

  val relationshipMappings = RelationshipMappings(getClass.getPackage.getName).mapping
}

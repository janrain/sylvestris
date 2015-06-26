package model

import graph._

object relationships {
  implicit object orgCustomer extends OneToOne[Organization, Customer]

  implicit object orgTree extends Tree[Organization]

  // TODO
  // representing a hierarchy will lead to implicit ambiguity for graph linking.
  // consider moving to separate graph and relationship layers.

  val relationshipMappings = RelationshipMappings(getClass.getPackage.getName).mapping
}

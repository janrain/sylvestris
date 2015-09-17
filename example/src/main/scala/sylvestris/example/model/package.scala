package sylvestris.example

import sylvestris.core._

package object model {
  // https://github.com/puffnfresh/wartremover/issues/149
  @SuppressWarnings(Array("org.brianmckenna.wartremover.warts.NonUnitStatements"))
  implicit object orgCustomer extends OneToOne[Organization, Customer]

  // https://github.com/puffnfresh/wartremover/issues/149
  @SuppressWarnings(Array("org.brianmckenna.wartremover.warts.NonUnitStatements"))
  implicit object orgTree extends Tree[Organization]
}

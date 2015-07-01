package sylvestris.example

import sylvestris.core._

package object model {
  implicit object orgCustomer extends OneToOne[Organization, Customer]
  implicit object orgTree extends Tree[Organization]
}

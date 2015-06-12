package model

import graph._

object relationships {
  implicit object orgCustomer extends OneToOne[Organization, Customer]
}

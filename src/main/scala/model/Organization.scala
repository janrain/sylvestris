package model

import graph._

object Organization {
  implicit object tag extends Tag[Organization]("org")
}

class Organization

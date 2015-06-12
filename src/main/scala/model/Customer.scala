package model

import graph._

object Customer {
  implicit object tag extends Tag[Customer]("cust")
}

class Customer

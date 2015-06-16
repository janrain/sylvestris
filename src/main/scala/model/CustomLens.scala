package model

import graph._, GraphM._
import model.relationships._
import spray.json._, DefaultJsonProtocol._

trait View[T, U] {
  def get(id: Id[T]): GraphM[Graph, U]
}

trait Update[T, U] {
  def update(id: Id[T], data: U): GraphM[Graph, U]
}

case class CustomData(orgName: String, customerName: String)

object CustomData {
  implicit val jsonFormat = jsonFormat2(apply)
}

object CustomLens extends View[Organization, CustomData] with Update[Organization, CustomData] {
  def get(id: Id[Organization]): GraphM[Graph, CustomData] =
    for {
      org <- lookupNode(id)
      // TODO get is bad!
      customer <- org.get.to[Customer]
    }
    yield CustomData(org.get.content.name, customer.get.content.name)


  def update(id: Id[Organization], data: CustomData): GraphM[Graph, CustomData] =
    for {
      orgOpt <- lookupNode(id)
      org = orgOpt.get
      customerOpt <- org.to[Customer]
      customer = customerOpt.get
      _ <- GraphM.update(org.copy(content = org.content.copy(name = data.orgName)))
      _ <- GraphM.update(customer.copy(content = customer.content.copy(name = data.customerName)))
    }
    yield data
}

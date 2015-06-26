package model

import graph._, GraphM._
import model.relationships._
import spray.json._, DefaultJsonProtocol._

trait View[T, U] {
  def get(id: Id): GraphM[U]
}

trait Update[T, U] {
  def update(id: Id, data: U): GraphM[U]
}

case class CustomData(orgName: String, customerName: String)

object CustomData {
  implicit val jsonFormat = jsonFormat2(apply)
}

object CustomLens extends View[Organization, CustomData] with Update[Organization, CustomData] {
  def get(id: Id): GraphM[CustomData] =
    for {
      org <- lookupNode[Organization](id)
      // TODO get is bad!
      customer <- org.get.to[Customer]
    }
    yield CustomData(org.get.content.name, customer.get.content.name)


  def update(id: Id, data: CustomData): GraphM[CustomData] =
    for {
      orgOpt <- lookupNode[Organization](id)
      org = orgOpt.get
      customerOpt <- org.to[Customer]
      customer = customerOpt.get
      _ <- updateNode(org.copy(content = org.content.copy(name = data.orgName)))
      _ <- updateNode(customer.copy(content = customer.content.copy(name = data.customerName)))
    }
    yield data
}

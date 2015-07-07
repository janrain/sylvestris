package sylvestris.example.model

import scalaz.{ \/, EitherT }
import scalaz.syntax.std.option._
import spray.json._, DefaultJsonProtocol._
import sylvestris.core._, GraphM._

case class CustomData(orgName: String, customerName: String)

object CustomData {
  implicit val jsonFormat = jsonFormat2(apply)
}

object CustomLens extends View[Organization, CustomData] with Update[Organization, CustomData] {
  def get(id: Id): EitherT[GraphM, Error, CustomData] =
    for {
      org <- getNode[Organization](id)
      customerOpt <- org.toOne[Customer]
      customer <- EitherT(GraphM(customerOpt.toRightDisjunction(Error("Customer not defined"))))
    }
    yield CustomData(org.content.name, customer.content.name)

  def update(id: Id, data: CustomData): EitherT[GraphM, Error, CustomData] =
    for {
      org <- getNode[Organization](id)
      customerOpt <- org.toOne[Customer]
      customer <- EitherT(GraphM(customerOpt.toRightDisjunction(Error("Customer not defined"))))
      _ <- updateNode(org.copy(content = org.content.copy(name = data.orgName)))
      _ <- updateNode(customer.copy(content = customer.content.copy(name = data.customerName)))
    }
    yield data
}

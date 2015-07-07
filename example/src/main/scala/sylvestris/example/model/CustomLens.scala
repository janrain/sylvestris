package sylvestris.example.model

import sylvestris.core._, GraphM._
import spray.json._, DefaultJsonProtocol._
import scalaz.{ Id => _, _ }, Scalaz.{ Id => _, _ }

case class CustomData(orgName: String, customerName: String)

object CustomData {
  implicit val jsonFormat = jsonFormat2(apply)
}

object CustomLens extends View[Organization, CustomData] with Update[Organization, CustomData] {
  def get(id: Id): GraphM[Error \/ CustomData] = {
    for {
      org <- getNode[Organization](id)
      customerOpt <- org.toOne[Customer]
      customer <- EitherT(GraphM(customerOpt.toRightDisjunction(Error("Customer not defined"))))
    }
    yield CustomData(org.content.name, customer.content.name)
  }.run

  // TODO : ripe for some EitherT sugar
  def update(id: Id, data: CustomData): GraphM[Error \/ CustomData] = {
    for {
      org <- getNode[Organization](id)
      customerOpt <- org.toOne[Customer]
      customer <- EitherT(GraphM(customerOpt.toRightDisjunction(Error("Customer not defined"))))
      _ <- updateNode(org.copy(content = org.content.copy(name = data.orgName)))
      _ <- updateNode(customer.copy(content = customer.content.copy(name = data.customerName)))
    }
    yield data
  }.run
}

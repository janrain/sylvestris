import graph._, GraphM._
import model._, relationships._

object example {

  val org: Node[Organization] = Node[Organization](Id("org"), Organization("Organization"))
  val customer: Node[Customer] = Node[Customer](Id("cust"), Customer("Customer"))

  val eg1 =
    for {
      _ <- addNode(org)
      _ <- addNode(customer)
      // TODO : relationships API
      // _ <- link(org, customer)
      foundOrg <- lookupNode[Organization](org.id).map(_.get)
      linkedCustomer <- foundOrg.to[Customer]
    } yield (foundOrg, linkedCustomer)

  // val addN1: Graph => Graph = add(n1)
  // val addN2: Graph => Graph = add(n2
  //
  // val addN1ThenN2: Graph => Graph = addN1 andThen addN2
  //
  // val doABunchOfStuff: Graph => Graph = ???
  // val graph = doABunchOfStuff(graph) // we might need to side effect to get the graph we need
  //
  // graph.get(theNodeIWant)
}

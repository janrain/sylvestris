package sylvestris.example.service

import akka.actor.{ ActorSystem, Props }
import spray.can.Http
import akka.io.IO
import sylvestris._, core._, Graph._, example.model._

object boot {

  def populate = {
    for {
      o1 <- addNode(Node[Organization](Id("org1"), Organization("Org 1")))
      o2 <- addNode(Node[Organization](Id("org2"), Organization("Org 2")))
      o3 <- addNode(Node[Organization](Id("org3"), Organization("Org 3")))
      c <- addNode(Node[Customer](Id("cust1"), Customer("Dave Corp.")))
      _ <- o1.toOne[Customer](Some(c))
      _ <- o2.tree.children(Set(o1))
      _ <- o3.tree.parent(Option(o1))
    }
    yield {}
  }.value

  @SuppressWarnings(Array("org.brianmckenna.wartremover.warts.NonUnitStatements"))
  def main(args: Array[String]): Unit = {

    val graph = InMemoryGraph()

    populate.run(graph)

    implicit val actorSystem = ActorSystem("service")

    val nodeRoutes = NodeRoutes(graph)

    val service = actorSystem.actorOf(
      Props(classOf[ServiceActor], nodeRoutes.routes, nodeRoutes.nodeWithRelationshipsOps, graph))

    IO(Http) ! Http.Bind(service, interface = "0.0.0.0", port = 8080)

  }

}

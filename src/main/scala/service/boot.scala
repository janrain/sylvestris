package service

import akka.actor.{ Actor, ActorSystem, Props }
import spray.can.Http
import akka.io.IO
import graph._, GraphM._
import model._, relationships._

object boot {

  def populate =
    for {
      o1 <- add(Node[Organization](Id("org1"), Organization("Org 1")))
      o2 <- add(Node[Organization](Id("org2"), Organization("Org 2")))
      c <- add(Node[Customer](Id("cust1"), Customer("Dave Corp.")))
      _ <- link(c, o1)
      _ <- link(o1, o2)
    }
    yield {}


  def main(args: Array[String]): Unit = {

    populate.run(InMemoryGraph)

    implicit val actorSystem = ActorSystem("service")

    val service = actorSystem.actorOf(Props(classOf[ServiceActor]))

    IO(Http) ! Http.Bind(service, interface = "0.0.0.0", port = 8080)

  }

}

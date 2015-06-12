package service

import akka.actor.{ Actor, ActorSystem, Props }
import spray.can.Http
import akka.io.IO
import graph._, GraphM._
import model._

object boot {

  def populate =
    for {
      o <- add(Node[Organization](Id("org1"), "some content"))
      c <- add(Node[Customer](Id("cust1"), "some content"))
      _ <- link(c, o)
      _ <- link(o, c)
    }
    yield {}


  def main(args: Array[String]): Unit = {

    populate.run(InMemoryGraph)

    implicit val actorSystem = ActorSystem("service")

    val service = actorSystem.actorOf(Props(classOf[ServiceActor]))

    IO(Http) ! Http.Bind(service, interface = "0.0.0.0", port = 8080)

  }

}

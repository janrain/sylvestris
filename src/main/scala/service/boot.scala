package service

import akka.actor.{ Actor, ActorSystem, Props }
import spray.can.Http
import akka.io.IO
import graph._, GraphM._
import model._

object boot {

  def populate =
    for {
      o1 <- addNode(Node[Organization](Id("org1"), Organization("Org 1")))
      o2 <- addNode(Node[Organization](Id("org2"), Organization("Org 2")))
      c <- addNode(Node[Customer](Id("cust1"), Customer("Dave Corp.")))
      // TODO : change to Relationship layer API
      // _ <- o1.to[Customer](Some(c))
      // _ <- o2.tree.children(Set(o1))
      //  OR
      // _ <- o1.tree.parent(Option(o2))
      _ <- addEdges(Set(
        Edge(None, c.id, NodeManifest[Customer].tag, o1.id, NodeManifest[Organization].tag),
        Edge(None, o1.id, NodeManifest[Organization].tag, c.id, NodeManifest[Customer].tag)))
    }
    yield {}


  def main(args: Array[String]): Unit = {

    populate.run(InMemoryGraph)

    implicit val actorSystem = ActorSystem("service")

    val service = actorSystem.actorOf(Props(classOf[ServiceActor]))

    IO(Http) ! Http.Bind(service, interface = "0.0.0.0", port = 8080)

  }

}

/*
* 
* For toMany relationships
* def apply[T](nodes: Set[T]) = clears if empty, else replaces
*
* For toOne relationships
* def apply[T](nodes: Option[T]) = clears if empty, else replaces
* 
* To relationships:
* * OneToOne
* * OneToMany
* * ManyToOne
* * ManyToMany
*
* Operations:
*
* Update
* Replace
* AddOrUpdate
* Remove
*
*
*/

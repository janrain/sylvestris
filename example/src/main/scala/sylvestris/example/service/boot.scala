package sylvestris.example.service

import akka.actor.{ Actor, ActorSystem, Props }
import spray.can.Http
import akka.io.IO
import scalaz.EitherT
import sylvestris._, core._, GraphM._, example.model._

object boot {

  def populate = {
    for {
      o1 <- EitherT(addNode(Node[Organization](Id("org1"), Organization("Org 1"))))
      o2 <- EitherT(addNode(Node[Organization](Id("org2"), Organization("Org 2"))))
      o3 <- EitherT(addNode(Node[Organization](Id("org3"), Organization("Org 3"))))
      c <- EitherT(addNode(Node[Customer](Id("cust1"), Customer("Dave Corp."))))
      _ <- EitherT(o1.toOne[Customer](Some(c)))
      _ <- EitherT(o2.tree.children(Set(o1)))
      _ <- EitherT(o3.tree.parent(Option(o1)))
    }
    yield {}
  }.run

  def main(args: Array[String]): Unit = {

    populate.run(InMemoryGraph)

    implicit val actorSystem = ActorSystem("service")

    val service = actorSystem.actorOf(
      Props(classOf[ServiceActor], NodeRoutes.nodeRoutes, NodeRoutes.nodeWithRelationshipsOps))

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

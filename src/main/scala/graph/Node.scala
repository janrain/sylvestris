package graph

import GraphM._

abstract class NodeOps[T : NodeManifest] {
  def node: Node[T]

  def to[U : NodeManifest : Relationship[T, ?]]: GraphM[Option[Node[U]]] =
    lookupEdges(node.id, Relationship[T, U].tNodeManifest.tag).flatMap { edges =>
      val nodes: Set[GraphM[Option[Node[U]]]] = edges.map { edge => lookupNode[U](edge.idB) }
      // TODO : head unsafe, solve later
      nodes.head
    }

  // def link[U](implicit ev1: Tag[T], ev2: NodeManifest[U], ev3: Relationship[T, U])
  //   : GraphM[Unit] = {
  //   // TODO expand this for other OneToMany/ManyToOne
  //
  //   ev3 match {
  //     case _: OneToOne[T, U]      => g.removeEdges[T, U](a.id)
  //     case _: Relationship[T, U]  =>
  //   }
  // }
  //


  // n1.update[Customer](cust3)
  //
  // n1.parent.get.children
  // n1.parent.put
  //
  // linkChildParent(c, p)
  // link[Children](p, c)
  //
  //
  // def parent(implicit ev1: Tag[T], ev2: NodeManifest[T], ev3: Parent[T])
  //
  // def child(implicit ev1: Tag[T], ev2: NodeManifest[T], ev3: Children[T])

}

object Node {
  implicit def toNodeOps[T : NodeManifest](n: Node[T]): NodeOps[T] = new NodeOps[T] {
    val node: Node[T] = n
  }
}

case class Node[T](id: Id, content: T)

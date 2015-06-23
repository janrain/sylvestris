package graph

import GraphM._
import spray.json._, DefaultJsonProtocol._

trait NodeOps[T] {
  def node: Node[T]

  def to[V](implicit ev1: Tag[T], ev2: NodeManifest[V], ev3: Relationship[T, V])
    : GraphM[Option[Node[V]]] =
    lookupEdges[T, V](node.id).flatMap { edges =>
      val nodes: Set[GraphM[Option[Node[V]]]] = edges.map { edge => lookupNode(edge.to) }
      // TODO : head unsafe, solve later
      nodes.head
    }
}

object Node {
  implicit def toNodeOps[T](n: Node[T]): NodeOps[T] = new NodeOps[T] {
    val node: Node[T] = n
  }

  implicit def jsonFormat[T : JsonFormat] = jsonFormat2(apply[T])

  implicit def existentialJsonFormat[T: JsonFormat]: JsonFormat[Node[_]] = jsonFormat2(apply[T])
}

case class Node[T](id: Id[T], content: T)

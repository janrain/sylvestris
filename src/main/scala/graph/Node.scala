package graph

import GraphM._
import spray.json._, DefaultJsonProtocol._

trait NodeOps[T] {
  def node: Node[T]

  def to[V](implicit ev1: Tag[T], ev2: Tag[V], ev3: Relationship[T, V])
    : GraphM[Graph, Option[Node[V]]] =
    lookupEdges[T, V](node.id).flatMap { edges =>
      val nodes: Set[GraphM[Graph, Option[Node[V]]]] = edges.map { edge => lookupNode(edge.to) }
      // TODO : head unsafe, solve later
      nodes.head
    }
}

object Node {
  implicit def toNodeOps[T](n: Node[T]): NodeOps[T] = new NodeOps[T] {
    val node: Node[T] = n
  }

  implicit def jsonFormat[T] = jsonFormat2(apply[T])

  implicit val existentialJsonFormat: JsonFormat[Node[_]] = jsonFormat2(apply)
}

case class Node[T](id: Id[T], content: String)

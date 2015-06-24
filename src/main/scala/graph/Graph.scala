package graph

import Graph._
import scalaz.Equal
import spray.json._

object Graph {
  case class GNode(id: String, tag: String, content: String)

  object GNode {
    def apply[T](node: Node[T])(implicit nm: NodeManifest[T]): GNode = {
      import nm.jsonFormat
      GNode(node.id.v, implicitly[Tag[T]].v, node.content.toJson.compactPrint)
    }
  }

  case class GEdge(label: Option[String], idA: String, tagA: String, idB: String, tagB: String)

  object GEdge {
    def apply[T: Tag, U: Tag](edge: Edge[T, U]): GEdge =
      GEdge(
        edge.label.map(_.v),
        edge.from.v, implicitly[Tag[T]].v,
        edge.to.v, implicitly[Tag[U]].v)

    implicit val eqInstance = Equal.equalA[GEdge]
  }
}

trait Graph {
  def nodes(): Set[Node[_]]
  def addNode[T : NodeManifest](node: Node[T]): Node[T]
  def updateNode[T : NodeManifest](node: Node[T]): Node[T]
  def removeNode[T : NodeManifest](id: Id[T]): Graph
  def edges(): Set[Edge[_, _]]
  def addEdge[T : Tag, U : Tag](edge: Edge[T, U]): Graph
  def addEdge(gedge: GEdge): Graph
  def removeEdge[T : Tag, U : Tag](edge: Edge[T, U]): Graph
  def removeEdges[T : Tag, U : Tag](id: Id[T]): Graph
  def removeEdges(id: String, tagA: String, tagB: String): Graph
  def lookupNode[T : NodeManifest](id: Id[T]): Option[Node[T]]
  def lookupEdges[T : Tag, U : Tag](id: Id[T]): Set[Edge[T, U]]
  def lookupEdges(id: String, tagA: String, tagB: String): Set[GEdge]
  def lookupEdgesAll[T : Tag](id: Id[T]): Set[Edge[T, _]]
}

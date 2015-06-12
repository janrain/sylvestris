package graph

import scalaz.Equal

trait Graph {
  case class GNode(id: String, tag: String, content: String)
  object GNode {
    def apply[T : Tag](node: Node[T]): GNode =
      GNode(node.id.v, implicitly[Tag[T]].v, node.content)
  }
  case class GEdge(idA: String, tagA: String, idB: String, tagB: String)
  object GEdge {
    def apply[T: Tag, U: Tag](edge: Edge[T, U]): GEdge =
      GEdge(
        edge.from.v, implicitly[Tag[T]].v,
        edge.to.v, implicitly[Tag[U]].v)

    implicit val eqInstance = Equal.equalA[GEdge]
  }

  def nodes(): Set[Node[_]]
  def addNode[T : Tag](node: Node[T]): Node[T]
  def updateNode[T : Tag](node: Node[T]): Node[T]
  def removeNode[T : Tag](id: Id[T]): Graph
  def edges(): Set[Edge[_, _]]
  def addEdge[T : Tag, U : Tag](edge: Edge[T, U]): Graph
  def removeEdge[T : Tag, U : Tag](edge: Edge[T, U]): Graph
  def lookupNode[T : Tag](id: Id[T]): Option[Node[T]]
  def lookupEdges[T : Tag, U : Tag](id: Id[T]): Set[Edge[T, U]]
  def lookupEdgesAll[T : Tag](id: Id[T]): Set[Edge[T, _]]
}

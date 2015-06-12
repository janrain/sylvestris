package graph

import scalaz.std._, string._, anyVal._
import scalaz.syntax.equal._

object InMemoryGraph extends Graph {
  // TODO Make gnodes a Map
  var gnodes: List[GNode] = Nil
  var gedges: Set[GEdge] = Set.empty

  def nodes(): Set[Node[_]] = gnodes.map(n => Node(Id(n.id), n.content)).toSet

  def addNode[T : Tag](node: Node[T]): Node[T] = {
    println(s"adding $node")
    gnodes +:= GNode(node)
    node
  }

  def updateNode[T : Tag](node: Node[T]): Node[T] = {
    println(s"updating $node")
    val index = gnodes.indexWhere(_.id === node.id.v)
    if (index === -1) sys.error("node not found")
    gnodes = gnodes.updated(index, GNode(node))
    node
  }

  def removeNode[T : Tag](id: Id[T]): Graph = {
    println(s"remove $id")
    gnodes = gnodes.filterNot(_.id === id.v)
    gedges = gedges.filterNot(e => e.idA === id.v || e.idB === id.v)
    this
  }

  def edges(): Set[Edge[_, _]] = gedges.map(e => Edge(Id(e.idA), Id(e.idB)))

  def addEdge[T : Tag, U : Tag](edge: Edge[T, U]): Graph = {
    println(s"adding $edge")
    gedges += GEdge(edge)
    this
  }

  def removeEdge[T : Tag, U : Tag](edge: Edge[T, U]): Graph = {
    println(s"remove $edge")
    gedges = gedges.filterNot(_ === GEdge(edge))
    this
  }

  // TODO check found type
  def lookupNode[T : Tag](id: Id[T]): Option[Node[T]] = gnodes.find(_.id === id.v).map {
    found =>
      Node[T](id, found.content)
  }

  def lookupEdges[T : Tag, U : Tag](id: Id[T]): Set[Edge[T, U]] =
    gedges
      .filter(e => e.idA === id.v && e.tagB === implicitly[Tag[U]].v)
      .map(e => Edge(id, Id[U](e.idB)))

  def lookupEdgesAll[T : Tag](id: Id[T]): Set[Edge[T, _]] =
    gedges
      .filter(e => e.idA === id.v)
      .map(e => Edge(id, Id(e.idB)))


}

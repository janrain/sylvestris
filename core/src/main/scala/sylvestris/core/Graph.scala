package sylvestris.core

import cats.data.XorT

object Graph {

  def nodes[T : NodeManifest](): XorT[GraphM, List[Error], Set[Node[T]]] =
    XorTGraphM(_.nodes[T]())

  def getNode[T : NodeManifest](id: Id): XorT[GraphM, Error, Node[T]] =
    XorTGraphM(_.getNode(id))

  def addNode[T : NodeManifest](node: Node[T]): XorT[GraphM, Error, Node[T]] =
    XorTGraphM(_.addNode(node))

  def updateNode[T : NodeManifest](node: Node[T]): XorT[GraphM, Error, Node[T]] =
    XorTGraphM(_.updateNode(node))

  def removeNode[T : NodeManifest](id: Id): XorT[GraphM, Error, Node[T]] =
    XorTGraphM(_.removeNode(id))

  def getEdges(id: Id, tag: Tag): XorT[GraphM, Error, Set[Edge]] =
    XorTGraphM(_.getEdges(id, tag))

  def getEdges(label: Option[Label], idA: Id, tagA: Tag, tagB: Tag): XorT[GraphM, Error, Set[Edge]] =
    XorTGraphM(_.getEdges(label, idA, tagA, tagB))

  def addEdges(edges: Set[Edge]): XorT[GraphM, Error, Set[Edge]] =
    XorTGraphM(_.addEdges(edges))

  def removeEdges(edges: Set[Edge]): XorT[GraphM, Error, Set[Edge]] =
    XorTGraphM(_.removeEdges(edges))

  def removeEdges(idA: Id, tagA: Tag, tagB: Tag): XorT[GraphM, Error, Set[Edge]] =
    XorTGraphM(_.removeEdges(idA, tagA, tagB))

}

trait Graph {
  def nodes[T : NodeManifest](): XorT[GraphM, List[Error], Set[Node[T]]]
  def getNode[T : NodeManifest](id: Id): XorT[GraphM, Error, Node[T]]
  def addNode[T : NodeManifest](node: Node[T]): XorT[GraphM, Error, Node[T]]
  def updateNode[T : NodeManifest](node: Node[T]): XorT[GraphM, Error, Node[T]]
  def removeNode[T : NodeManifest](id: Id): XorT[GraphM, Error, Node[T]]
  def getEdges(id: Id, tag: Tag): XorT[GraphM, Error, Set[Edge]]
  def getEdges(label: Option[Label], idA: Id, tagA: Tag, tagB: Tag): XorT[GraphM, Error, Set[Edge]]
  def addEdges(edges: Set[Edge]): XorT[GraphM, Error, Set[Edge]]
  def removeEdges(edges: Set[Edge]): XorT[GraphM, Error, Set[Edge]]
  def removeEdges(idA: Id, tagA: Tag, tagB: Tag): XorT[GraphM, Error, Set[Edge]]
}

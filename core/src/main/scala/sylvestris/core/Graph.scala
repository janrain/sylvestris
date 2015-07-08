package sylvestris.core

import scalaz.{ \/, EitherT }

object Graph {

  def nodes[T : NodeManifest](): EitherT[GraphM, List[Error], Set[Node[T]]] =
    EitherTGraphM(_.nodes[T]())

  def getNode[T : NodeManifest](id: Id): EitherT[GraphM, Error, Node[T]] =
    EitherTGraphM(_.getNode(id))

  def addNode[T : NodeManifest](node: Node[T]): EitherT[GraphM, Error, Node[T]] =
    EitherTGraphM(_.addNode(node))

  def updateNode[T : NodeManifest](node: Node[T]): EitherT[GraphM, Error, Node[T]] =
    EitherTGraphM(_.updateNode(node))

  def removeNode[T : NodeManifest](id: Id): EitherT[GraphM, Error, Node[T]] =
    EitherTGraphM(_.removeNode(id))

  def getEdges(id: Id, tag: Tag): EitherT[GraphM, Error, Set[Edge]] =
    EitherTGraphM(_.getEdges(id, tag))

  def getEdges(label: Option[Label], idA: Id, tagA: Tag, tagB: Tag): EitherT[GraphM, Error, Set[Edge]] =
    EitherTGraphM(_.getEdges(label, idA, tagA, tagB))

  def addEdges(edges: Set[Edge]): EitherT[GraphM, Error, Set[Edge]] =
    EitherTGraphM(_.addEdges(edges))

  def removeEdges(edges: Set[Edge]): EitherT[GraphM, Error, Set[Edge]] =
    EitherTGraphM(_.removeEdges(edges))

  def removeEdges(idA: Id, tagA: Tag, tagB: Tag): EitherT[GraphM, Error, Set[Edge]] =
    EitherTGraphM(_.removeEdges(idA, tagA, tagB))

}

trait Graph {
  def nodes[T : NodeManifest](): EitherT[GraphM, List[Error], Set[Node[T]]]
  def getNode[T : NodeManifest](id: Id): EitherT[GraphM, Error, Node[T]]
  def addNode[T : NodeManifest](node: Node[T]): EitherT[GraphM, Error, Node[T]]
  def updateNode[T : NodeManifest](node: Node[T]): EitherT[GraphM, Error, Node[T]]
  def removeNode[T : NodeManifest](id: Id): EitherT[GraphM, Error, Node[T]]
  def getEdges(id: Id, tag: Tag): EitherT[GraphM, Error, Set[Edge]]
  def getEdges(label: Option[Label], idA: Id, tagA: Tag, tagB: Tag): EitherT[GraphM, Error, Set[Edge]]
  def addEdges(edges: Set[Edge]): EitherT[GraphM, Error, Set[Edge]]
  def removeEdges(edges: Set[Edge]): EitherT[GraphM, Error, Set[Edge]]
  def removeEdges(idA: Id, tagA: Tag, tagB: Tag): EitherT[GraphM, Error, Set[Edge]]
}

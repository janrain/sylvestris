package sylvestris.core

import scalaz.\/

trait Graph {
  def nodes[T : NodeManifest](): List[Error] \/ Set[Node[T]]
  def getNode[T : NodeManifest](id: Id): Error \/ Node[T]
  def addNode[T : NodeManifest](node: Node[T]): Error \/ Node[T]
  def updateNode[T : NodeManifest](node: Node[T]): Error \/ Node[T]
  def removeNode[T : NodeManifest](id: Id): Error \/ Node[T]
  def getEdges(id: Id, tag: Tag): Error \/ Set[Edge]
  def getEdges(label: Option[Label], idA: Id, tagA: Tag, tagB: Tag): Error \/ Set[Edge]
  def addEdges(edges: Set[Edge]): Error \/ Set[Edge]
  def removeEdges(edges: Set[Edge]): Error \/ Set[Edge]
  def removeEdges(idA: Id, tagA: Tag, tagB: Tag): Error \/ Set[Edge]
}

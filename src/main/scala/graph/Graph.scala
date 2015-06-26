package graph

// TODO : revisit the returning of Graph; might want \/ of some sort

trait Graph {
  def nodes[T : NodeManifest](): Set[Node[T]]
  def lookupNode[T : NodeManifest](id: Id): Option[Node[T]]
  def addNode[T : NodeManifest](node: Node[T]): Node[T]
  def updateNode[T : NodeManifest](node: Node[T]): Node[T]
  def removeNode[T : NodeManifest](id: Id): Graph
  def lookupEdges(id: Id, tag: Tag): Set[Edge]
  def lookupEdges(idA: Id, tagA: Tag, tagB: Tag): Set[Edge]
  def addEdges(edges: Set[Edge]): Graph
  def removeEdges(edges: Set[Edge]): Graph
  def removeEdges(idA: Id, tagA: Tag, tagB: Tag): Graph
}

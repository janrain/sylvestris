package graph

trait GraphM[T] {
  def run: Graph => T
  def map[U](f: T => U): GraphM[U] = GraphM(i => f(run(i)))
  def flatMap[U](f: T => GraphM[U]): GraphM[U] = GraphM(g => f(run(g)).run(g))
}

object GraphM {

    def apply[T](v: Graph => T) = new GraphM[T] { def run: Graph => T = v }

    def apply[T](v: T) = new GraphM[T] { def run: Graph => T = _ => v }

    def sequence[T](l: Iterable[GraphM[T]]): GraphM[Iterable[T]] = GraphM(g => l.map(_.run(g)))

    def nodes[T : NodeManifest](): GraphM[Set[Node[T]]] = GraphM(_.nodes[T]())

    def lookupNode[T : NodeManifest](id: Id): GraphM[Option[Node[T]]] = GraphM(_.lookupNode(id))

    def addNode[T : NodeManifest](node: Node[T]): GraphM[Node[T]] = GraphM(_.addNode(node))

    def updateNode[T : NodeManifest](node: Node[T]): GraphM[Node[T]] = GraphM(_.updateNode(node))

    def removeNode[T : NodeManifest](node: Id): GraphM[Graph] = GraphM(_.removeNode(node))

    def lookupEdges(id: Id, tag: Tag): GraphM[Set[Edge]] =
      GraphM(_.lookupEdges(id, tag))

    def lookupEdges(label: Option[Label], idA: Id, tagA: Tag, tagB: Tag): GraphM[Set[Edge]] =
      GraphM(_.lookupEdges(label, idA, tagA, tagB))

    def addEdges(edges: Set[Edge]): GraphM[Graph] = GraphM(_.addEdges(edges))

    def removeEdges(edges: Set[Edge]): GraphM[Graph] = GraphM(_.removeEdges(edges))

    def removeEdges(idA: Id, tagA: Tag, tagB: Tag): GraphM[Graph] = GraphM(_.removeEdges(idA, tagA, tagB))

}

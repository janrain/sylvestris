package sylvestris.core

import scalaz.{ \/, Monad }

trait GraphM[T] {
  def run: Graph => T
  def map[U](f: T => U): GraphM[U] = GraphM(i => f(run(i)))
  def flatMap[U](f: T => GraphM[U]): GraphM[U] = GraphM(g => f(run(g)).run(g))
}

object GraphM {

  implicit object monadInstance extends Monad[GraphM] {
    def point[A](a: => A): GraphM[A] = new GraphM[A] { def run: Graph => A = g => a }
    def bind[A, B](fa: GraphM[A])(f: A => GraphM[B]): GraphM[B] = fa.flatMap(f)
  }

  def apply[T](v: Graph => T) = new GraphM[T] { def run: Graph => T = v }

  def apply[T](v: T) = new GraphM[T] { def run: Graph => T = _ => v }

  def sequence[T](l: Iterable[GraphM[T]]): GraphM[Iterable[T]] = GraphM(g => l.map(_.run(g)))

  def nodes[T : NodeManifest](): GraphM[List[Error] \/ Set[Node[T]]] = GraphM(_.nodes[T]())

  def getNode[T : NodeManifest](id: Id): GraphM[Error \/ Node[T]] = GraphM(_.getNode(id))

  def addNode[T : NodeManifest](node: Node[T]): GraphM[Error \/ Node[T]] = GraphM(_.addNode(node))

  def updateNode[T : NodeManifest](node: Node[T]): GraphM[Error \/ Node[T]] = GraphM(_.updateNode(node))

  def removeNode[T : NodeManifest](node: Id): GraphM[Error \/ Node[T]] = GraphM(_.removeNode(node))

  def getEdges(id: Id, tag: Tag): GraphM[Error \/ Set[Edge]] = GraphM(_.getEdges(id, tag))

  def getEdges(label: Option[Label], idA: Id, tagA: Tag, tagB: Tag): GraphM[Error \/ Set[Edge]] =
    GraphM(_.getEdges(label, idA, tagA, tagB))

  def addEdges(edges: Set[Edge]): GraphM[Error \/ Set[Edge]] = GraphM(_.addEdges(edges))

  def removeEdges(edges: Set[Edge]): GraphM[Error \/ Set[Edge]] = GraphM(_.removeEdges(edges))

  def removeEdges(idA: Id, tagA: Tag, tagB: Tag): GraphM[Error \/ Set[Edge]] =
    GraphM(_.removeEdges(idA, tagA, tagB))

}

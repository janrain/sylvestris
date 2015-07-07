package sylvestris.core

import scalaz.{ EitherT, Monad }
import scalaz.Scalaz._

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

  def sequence[T, U](l: Iterable[EitherT[GraphM, T, U]]): EitherT[GraphM, T, Iterable[U]] =
    EitherT(GraphM(g => l.map(_.run.run(g)).toList.sequenceU))

  def nodes[T : NodeManifest](): EitherT[GraphM, List[Error], Set[Node[T]]] = EitherT(GraphM(_.nodes[T]()))

  def getNode[T : NodeManifest](id: Id): EitherT[GraphM, Error, Node[T]] = EitherT(GraphM(_.getNode(id)))

  def addNode[T : NodeManifest](node: Node[T]): EitherT[GraphM, Error, Node[T]] = EitherT(GraphM(_.addNode(node)))

  def updateNode[T : NodeManifest](node: Node[T]): EitherT[GraphM, Error, Node[T]] = EitherT(GraphM(_.updateNode(node)))

  def removeNode[T : NodeManifest](node: Id): EitherT[GraphM, Error, Node[T]] = EitherT(GraphM(_.removeNode(node)))

  def getEdges(id: Id, tag: Tag): EitherT[GraphM, Error, Set[Edge]] = EitherT(GraphM(_.getEdges(id, tag)))

  def getEdges(label: Option[Label], idA: Id, tagA: Tag, tagB: Tag): EitherT[GraphM, Error, Set[Edge]] =
    EitherT(GraphM(_.getEdges(label, idA, tagA, tagB)))

  def addEdges(edges: Set[Edge]): EitherT[GraphM, Error, Set[Edge]] = EitherT(GraphM(_.addEdges(edges)))

  def removeEdges(edges: Set[Edge]): EitherT[GraphM, Error, Set[Edge]] = EitherT(GraphM(_.removeEdges(edges)))

  def removeEdges(idA: Id, tagA: Tag, tagB: Tag): EitherT[GraphM, Error, Set[Edge]] =
    EitherT(GraphM(_.removeEdges(idA, tagA, tagB)))

}

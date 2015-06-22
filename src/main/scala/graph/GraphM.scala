package graph

import Graph._
import scala.collection.generic.CanBuildFrom
import spray.json._

// the monad appears
// TODO rename this
trait GraphM[T, U] {
  def run: T => U
  def map[V](f: U => V): GraphM[T, V] = GraphM(i => f(run(i)))
  def flatMap[V](f: U => GraphM[T, V]): GraphM[T, V] = GraphM(g => f(run(g)).run(g))
}

object GraphM {

    def apply[T, U](v: T => U) = new GraphM[T, U] { def run: T => U = v }

    def apply[T](v: T) = new GraphM[Graph, T] { def run: Graph => T = _ => v }

    def sequence[T](l: Iterable[GraphM[Graph, T]]): GraphM[Graph, Iterable[T]] = GraphM(g => l.map(_.run(g)))

    def nodes(): GraphM[Graph, Set[Node[_]]] = GraphM(g => g.nodes())

    def add[T : Tag : JsonFormat](node: Node[T]): GraphM[Graph, Node[T]] = GraphM(g => g.addNode(node))

    def update[T : Tag : JsonFormat](node: Node[T]): GraphM[Graph, Node[T]] = GraphM(g => g.updateNode(node))

    def link[T : Tag, U : Tag](a: Node[T], b: Node[U]): GraphM[Graph, Graph] = GraphM { g =>
      g.addEdge(Edge(a.id, b.id))
      g.addEdge(Edge(b.id, a.id))
    }

    def link(idA: String, tagA: String, idB: String, tagB: String): GraphM[Graph, Graph] = GraphM { g =>
      g.addEdge(GEdge(idA, tagA, idB, tagB))
      g.addEdge(GEdge(idB, tagB, idA, tagA))
    }

    def remove[T : Tag](node: Id[T]): GraphM[Graph, Graph] = GraphM(g => g.removeNode(node))

    def unlink[T : Tag, U : Tag](a: Node[T], b: Node[U]): GraphM[Graph, Graph] =
      GraphM(g => g.removeEdge(Edge(a.id, b.id)))

    def lookupNode[T : Tag : JsonFormat](id: Id[T]): GraphM[Graph, Option[Node[T]]] = GraphM(g => g.lookupNode(id))

    def lookupEdges[T : Tag, U : Tag](id: Id[T]): GraphM[Graph, Set[Edge[T, U]]] =
      GraphM(g => g.lookupEdges(id))

    def lookupEdgesAll[T : Tag](id: Id[T]): GraphM[Graph, Set[Edge[T, _]]] =
      GraphM(g => g.lookupEdgesAll(id))

}

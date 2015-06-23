package graph

import Graph._
import scala.collection.generic.CanBuildFrom
import scalaz.syntax.equal._
import scalaz.std.string._
import spray.json._

trait GraphM[T] {
  def run: Graph => T
  def map[U](f: T => U): GraphM[U] = GraphM(i => f(run(i)))
  def flatMap[U](f: T => GraphM[U]): GraphM[U] = GraphM(g => f(run(g)).run(g))
}

object GraphM {

    def apply[T](v: Graph => T) = new GraphM[T] { def run: Graph => T = v }

    def apply[T](v: T) = new GraphM[T] { def run: Graph => T = _ => v }

    def sequence[T](l: Iterable[GraphM[T]]): GraphM[Iterable[T]] = GraphM(g => l.map(_.run(g)))

    def nodes(): GraphM[Set[Node[_]]] = GraphM(g => g.nodes())

    def add[T : Tag : JsonFormat](node: Node[T]): GraphM[Node[T]] = GraphM(g => g.addNode(node))

    def update[T : Tag : JsonFormat](node: Node[T]): GraphM[Node[T]] = GraphM(g => g.updateNode(node))

    // TODO expand this for other OneToMany/ManyToOne
    def link[T : Tag, U : Tag](a: Node[T], b: Node[U])(implicit ev: Relationship[T, U]): GraphM[Graph] = GraphM { g =>
      ev match {
        case _: OneToOne[T, U]      => g.removeEdges[T, U](a.id)
        case _: Relationship[T, U]  =>
      }

      g.addEdge(Edge(a.id, b.id))
      g.addEdge(Edge(b.id, a.id))
    }

    // TODO expand this for other OneToMany/ManyToOne
    def link
      (idA: String, tagA: String, idB: String, tagB: String)
      (relationships: List[Relationship[_, _]])
      : GraphM[Graph] = GraphM { g =>
      relationships.find(_.uTag.v === tagB) match {
        case Some(_ : OneToOne[_, _])     =>
          g.removeEdges(idA, tagA, tagB)
          // for all x nodes b links to, remove edges from x to tagB
          g.lookupEdges(idB, tagB, tagA).foreach(e => g.removeEdges(e.idB, tagA, tagB))
          g.removeEdges(idB, tagB, tagA)
        case Some(_ : Relationship[_, _]) =>
        case None => sys.error(s"no relationship between $tagA and $tagB")
      }
      g.addEdge(GEdge(idA, tagA, idB, tagB))
      g.addEdge(GEdge(idB, tagB, idA, tagA))
    }

    def remove[T : Tag](node: Id[T]): GraphM[Graph] = GraphM(g => g.removeNode(node))

    def unlink[T : Tag, U : Tag](a: Node[T], b: Node[U]): GraphM[Graph] =
      GraphM(g => g.removeEdge(Edge(a.id, b.id)))

    def lookupNode[T : Tag : JsonFormat](id: Id[T]): GraphM[Option[Node[T]]] = GraphM(g => g.lookupNode(id))

    def lookupEdges[T : Tag, U : Tag](id: Id[T]): GraphM[Set[Edge[T, U]]] =
      GraphM(g => g.lookupEdges(id))

    def lookupEdgesAll[T : Tag](id: Id[T]): GraphM[Set[Edge[T, _]]] =
      GraphM(g => g.lookupEdgesAll(id))

}

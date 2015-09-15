package sylvestris.core

import cats.data._
import cats.implicits._
import catsclaw.implicits._
import spray.json._

object InMemoryGraph {
  def apply() = new InMemoryGraph {}
}

trait InMemoryGraph extends Graph {
  case class GNode(id: Id, tag: Tag, content: String)

  object GNode {
    def apply[T : NodeManifest](node: Node[T]): GNode = {
      GNode(node.id, NodeManifest[T].tag, node.content.toJson(NodeManifest[T].jsonFormat).compactPrint)
    }
  }

  var gnodes: Map[Id, GNode] = Map()
  var gedges: Set[Edge] = Set.empty

  def parseNode[T : NodeManifest](v: GNode): Error Xor Node[T] =
    Xor.fromTryCatch(v.content.parseJson.convertTo[T](NodeManifest[T].jsonFormat))
      .bimap(t => Error(s"unable to parse $v to Node", Some(t)), Node[T](v.id, _))

  def nodes[T : NodeManifest](): XorT[GraphM, List[Error], Set[Node[T]]] = XorTGraphM {
    gnodes
      .collect {
        case (_, gnode) if gnode.tag === NodeManifest[T].tag => parseNode(gnode).bimap(List(_), Set(_))
      }
      .toList
      .combineAll
  }

  def getNode[T : NodeManifest](id: Id): XorT[GraphM, Error, Node[T]] = XorTGraphM {
    gnodes
      .values
      .find(n => n.id === id && n.tag === NodeManifest[T].tag)
      .toRightXor(Error(s"$id not found"))
      .flatMap(parseNode[T])
  }

  def addNode[T : NodeManifest](node: Node[T]): XorT[GraphM, Error, Node[T]] = XorTGraphM {
    gnodes += node.id -> GNode(node)
    node.right
  }

  def updateNode[T : NodeManifest](node: Node[T]): XorT[GraphM, Error, Node[T]] = XorTGraphM {
    gnodes
      .get(node.id)
      .map { n => gnodes += node.id -> GNode(node); node }
      .toRightXor(Error("node not found"))
  }

  def removeNode[T : NodeManifest](id: Id): XorT[GraphM, Error, Node[T]] = XorTGraphM {
    val tag = NodeManifest[T].tag
    val node = gnodes.get(id)
    gnodes -= id
    gedges = gedges.filterNot(e => (e.idA === id && e.tagA === tag) || (e.idB === id && e.tagB === tag))
    node
      .toRightXor(Error("node not found"))
      .flatMap(parseNode[T])
  }

  def getEdges(id: Id, tag: Tag): XorT[GraphM, Error, Set[Edge]] = XorTGraphM {
    gedges.filter(e => e.idA === id && e.tagA === tag).right
  }

  def getEdges(label: Option[Label], idA: Id, tagA: Tag, tagB: Tag): XorT[GraphM, Error, Set[Edge]] =
    XorTGraphM {
      gedges.filter(e => e.idA === idA && e.tagA === tagA && e.tagB === tagB && e.label === label).right
    }


  def addEdges(edges: Set[Edge]): XorT[GraphM, Error, Set[Edge]] = XorTGraphM {
    gedges ++= edges
    edges.right
  }

  def removeEdges(edges: Set[Edge]): XorT[GraphM, Error, Set[Edge]] = XorTGraphM {
    gedges = gedges -- edges
    edges.right
  }

  def removeEdges(idA: Id, tagA: Tag, tagB: Tag): XorT[GraphM, Error, Set[Edge]] = XorTGraphM {
    val removedGedges = gedges.filter(e => e.idA === idA && e.tagA === tagA && e.tagB === tagB)
    gedges --= removedGedges
    removedGedges.right
  }

}

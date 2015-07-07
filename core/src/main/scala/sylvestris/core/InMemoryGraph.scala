package sylvestris.core

import scalaz.{ \/, EitherT }
import scalaz.Scalaz._
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

  def parseNode[T : NodeManifest](v: GNode): Error \/ Node[T] =
    \/.fromTryCatchNonFatal(v.content.parseJson.convertTo[T](NodeManifest[T].jsonFormat))
      .bimap(t => Error(s"unable to parse $v to Node", Some(t)), Node[T](v.id, _))

  def nodes[T : NodeManifest](): EitherT[GraphM, List[Error], Set[Node[T]]] = EitherTGraphM {
    gnodes
      .collect {
        case (_, gnode) if gnode.tag === NodeManifest[T].tag => parseNode(gnode).bimap(List(_), Set(_))
      }
      .toList
      .suml
  }

  def getNode[T : NodeManifest](id: Id): EitherT[GraphM, Error, Node[T]] = EitherTGraphM {
    gnodes
      .values
      .find(n => n.id === id && n.tag === NodeManifest[T].tag)
      .toRightDisjunction(Error(s"$id not found"))
      .flatMap(parseNode[T])
  }

  def addNode[T : NodeManifest](node: Node[T]): EitherT[GraphM, Error, Node[T]] = EitherTGraphM {
    gnodes += node.id -> GNode(node)
    node.right
  }

  def updateNode[T : NodeManifest](node: Node[T]): EitherT[GraphM, Error, Node[T]] = EitherTGraphM {
    gnodes
      .get(node.id)
      .map { n => gnodes += node.id -> GNode(node); node }
      .toRightDisjunction(Error("node not found"))
  }

  def removeNode[T : NodeManifest](id: Id): EitherT[GraphM, Error, Node[T]] = EitherTGraphM {
    val tag = NodeManifest[T].tag
    val node = gnodes.get(id)
    gnodes -= id
    gedges = gedges.filterNot(e => (e.idA === id && e.tagA === tag) || (e.idB === id && e.tagB === tag))
    node
      .toRightDisjunction(Error("node not found"))
      .flatMap(parseNode[T])
  }

  def getEdges(id: Id, tag: Tag): EitherT[GraphM, Error, Set[Edge]] = EitherTGraphM {
    gedges.filter(e => e.idA === id && e.tagA === tag).right
  }

  def getEdges(label: Option[Label], idA: Id, tagA: Tag, tagB: Tag): EitherT[GraphM, Error, Set[Edge]] =
    EitherTGraphM {
      gedges.filter(e => e.idA === idA && e.tagA === tagA && e.tagB === tagB && e.label === label).right
    }


  def addEdges(edges: Set[Edge]): EitherT[GraphM, Error, Set[Edge]] = EitherTGraphM {
    gedges ++= edges
    edges.right
  }

  def removeEdges(edges: Set[Edge]): EitherT[GraphM, Error, Set[Edge]] = EitherTGraphM {
    gedges = gedges -- edges
    edges.right
  }

  def removeEdges(idA: Id, tagA: Tag, tagB: Tag): EitherT[GraphM, Error, Set[Edge]] = EitherTGraphM {
    val removedGedges = gedges.filter(e => e.idA === idA && e.tagA === tagA && e.tagB === tagB)
    gedges --= removedGedges
    removedGedges.right
  }

}

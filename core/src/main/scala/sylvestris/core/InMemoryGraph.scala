package sylvestris.core

import scalaz._, Scalaz._
import spray.json._

object InMemoryGraph extends Graph {
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

  def nodes[T : NodeManifest](): List[Error] \/ Set[Node[T]] = gnodes
    .collect {
      case (_, gnode) if gnode.tag === NodeManifest[T].tag => parseNode(gnode).bimap(List(_), Set(_))
    }
    .toList
    .suml

  // TODO check found type
  def getNode[T : NodeManifest](id: Id): Error \/ Node[T] = gnodes
    .values
    .find(n => n.id === id && n.tag === NodeManifest[T].tag)
    .toRightDisjunction(Error(s"$id not found"))
    .flatMap(parseNode[T])


  def addNode[T : NodeManifest](node: Node[T]): Error \/ Node[T] = {
    println(s"adding $node")
    gnodes += node.id -> GNode(node)
    node.right
  }

  def updateNode[T : NodeManifest](node: Node[T]): Error \/ Node[T] = {
    println(s"updating $node")
    gnodes
      .get(node.id)
      .map { n => gnodes += node.id -> GNode(node); node }
      .toRightDisjunction(Error("node not found"))
  }

  def removeNode[T : NodeManifest](id: Id): Error \/ Node[T] = {
    println(s"remove $id")
    val tag = NodeManifest[T].tag
    val node = gnodes.get(id)
    gnodes -= id
    gedges = gedges.filterNot(e => (e.idA === id && e.tagA === tag) || (e.idB === id && e.tagB === tag))
    node
      .toRightDisjunction(Error("node not found"))
      .flatMap(parseNode[T])
  }

  def getEdges(id: Id, tag: Tag): Error \/ Set[Edge] =
    gedges.filter(e => e.idA === id && e.tagA === tag).right

  def getEdges(label: Option[Label], idA: Id, tagA: Tag, tagB: Tag): Error \/ Set[Edge] =
    gedges.filter(e => e.idA === idA && e.tagA === tagA && e.tagB === tagB && e.label === label).right


  def addEdges(edges: Set[Edge]): Error \/ Set[Edge] = {
    println(s"adding $edges")
    gedges ++= edges
    edges.right
  }

  def removeEdges(edges: Set[Edge]): Error \/ Set[Edge] = {
    println(s"remove $edges")
    gedges = gedges -- edges
    edges.right
  }

  def removeEdges(idA: Id, tagA: Tag, tagB: Tag): Error \/ Set[Edge] = {
    println(s"removing edges for $idA, $tagA, $tagB")
    val removedGedges = gedges.filter(e => e.idA === idA && e.tagA === tagA && e.tagB === tagB)
    gedges --= removedGedges
    removedGedges.right
  }

}

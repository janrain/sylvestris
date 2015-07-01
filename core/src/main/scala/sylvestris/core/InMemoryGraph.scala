package sylvestris.core

import scalaz.std.anyVal._
import scalaz.std.option._
import scalaz.syntax.equal._
import spray.json._

object InMemoryGraph extends Graph {
  case class GNode(id: Id, tag: Tag, content: String)

  object GNode {
    def apply[T : NodeManifest](node: Node[T]): GNode = {
      GNode(node.id, NodeManifest[T].tag, node.content.toJson(NodeManifest[T].jsonFormat).compactPrint)
    }
  }

  // TODO Make gnodes a Map
  var gnodes: List[GNode] = Nil
  var gedges: Set[Edge] = Set.empty

  def nodes[T : NodeManifest](): Set[Node[T]] = gnodes
    .filter(_.tag === NodeManifest[T].tag)
    .map(n => Node[T](n.id, n.content.parseJson.convertTo[T](NodeManifest[T].jsonFormat)))
    .toSet

  // TODO check found type
  def getNode[T : NodeManifest](id: Id): Option[Node[T]] = {
    gnodes.find(n => n.id === id && n.tag === NodeManifest[T].tag).map {
      found => Node[T](id, found.content.parseJson.convertTo[T](NodeManifest[T].jsonFormat))
    }
  }

  def addNode[T : NodeManifest](node: Node[T]): Node[T] = {
    println(s"adding $node")
    gnodes +:= GNode(node)
    node
  }

  def updateNode[T : NodeManifest](node: Node[T]): Node[T] = {
    println(s"updating $node")
    val index = gnodes.indexWhere(_.id === node.id)
    if (index === -1) sys.error("node not found")
    gnodes = gnodes.updated(index, GNode(node))
    node
  }

  def removeNode[T : NodeManifest](id: Id): Graph = {
    println(s"remove $id")
    val tag = NodeManifest[T].tag
    gnodes = gnodes.filterNot(n => n.id === id && n.tag === tag)
    gedges = gedges.filterNot(e => (e.idA === id && e.tagA === tag) || (e.idB === id && e.tagB === tag))
    this
  }

  def getEdges(id: Id, tag: Tag): Set[Edge] =
    gedges.filter(e => e.idA === id && e.tagA === tag)

  def getEdges(label: Option[Label], idA: Id, tagA: Tag, tagB: Tag): Set[Edge] =
    gedges.filter(e => e.idA === idA && e.tagA === tagA && e.tagB === tagB && e.label === label)


  def addEdges(edges: Set[Edge]): Graph = {
    println(s"adding $edges")
    gedges ++= edges
    this
  }

  def removeEdges(edges: Set[Edge]): Graph = {
    println(s"remove $edges")
    gedges = gedges -- edges
    this
  }

  def removeEdges(idA: Id, tagA: Tag, tagB: Tag): Graph = {
    println(s"removing edges for $idA, $tagA, $tagB")
    gedges = gedges.filterNot(e => e.idA === idA && e.tagA === tagA && e.tagB === tagB)
    this
  }

}

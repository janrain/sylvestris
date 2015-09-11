package sylvestris.service.common

import cats.data._
import cats.implicits._
import sylvestris._, core._, Graph._

// TODO : supply pathSegmentToTag and tagToPathSegment in another way?

object NodeWithRelationshipsOps {
  case class Relationship(id: Id, tag: Tag, label: Option[Label])

  val NodePathExtractor = "/api/(.*?)/(.*)".r
}

case class NodeWithRelationshipsOps(
  relationshipMappings: Map[Tag, List[core.Relationship[_, _]]],
  pathSegmentToTag: Map[PathSegment[_], Tag],
  tagToPathSegment: Map[Tag, PathSegment[_]]) {

  def splitNodePath(nodePath: String): Error Xor (Tag, Id) = {
    val invalidNodePath = Error(s"invalid node path $nodePath")
    nodePath match {
      case NodeWithRelationshipsOps.NodePathExtractor(pathSegment, id) =>
        pathSegmentToTag
          .get(PathSegment(pathSegment))
          .map(tag => (tag, Id(id)))
          .toRightXor(invalidNodePath)
      case _ => invalidNodePath.left
    }
  }

  def edgeToRelationship(edge: Edge): Error Xor Relationship =
    tagToPathSegment
      .get(edge.tagB)
      .map(pathSegment => Relationship(edge.label.map(_.v), s"/api/${pathSegment.v}/${edge.idB.v}"))
      .toRightXor(Error(s"path segment not found for ${edge.tagB}"))

  def nodeWithRelationships[T : NodeManifest](id: Id): XorT[GraphM, Error, NodeWithRelationships[T]] =
    for {
      node <- getNode(id)
      edges <- getEdges(id, NodeManifest[T].tag)
      relationships <- XorT(GraphM(edges.map(edgeToRelationship).toList.sequenceU))
    }
    yield NodeWithRelationships[T](node, relationships.toSet)

  def addNodeWithRelationships[T](nodeWithRelationships: NodeWithRelationships[T])(implicit nm: NodeManifest[T])
    : XorT[GraphM, List[Error], NodeWithRelationships[T]] =
    for {
      n <- addNode(nodeWithRelationships.node).leftMap(List(_))
      v <- setRelationships(n, nodeWithRelationships.relationships)
    }
    yield nodeWithRelationships

  def parseRelationship(relationship: Relationship): Error Xor NodeWithRelationshipsOps.Relationship =
    splitNodePath(relationship.nodePath)
      .map {
        case (tag, id) => NodeWithRelationshipsOps.Relationship(id, tag, relationship.label.map(Label(_)))
      }

  def parseRelationships(relationships: Set[Relationship])
    : List[Error] Xor Set[NodeWithRelationshipsOps.Relationship] =
    relationships
      .toList
      .traverseU(parseRelationship(_).leftMap(List(_)))
      .map(_.toSet)

  def groupRelationships(
    relationships: Set[NodeWithRelationshipsOps.Relationship],
    legalRelationshipMapping: Map[Tag, List[core.Relationship[_, _]]])
    : Map[core.Relationship[_, _], Set[NodeWithRelationshipsOps.Relationship]]
    = {
      val groupedRelationships = relationships.groupBy(_.tag)

      val i: List[Map[core.Relationship[_, _], Set[NodeWithRelationshipsOps.Relationship]]] = {
        for {
          (tag, relationships) <- legalRelationshipMapping
          relationship <- relationships
        } yield Map[core.Relationship[_, _], Set[NodeWithRelationshipsOps.Relationship]] {
          relationship -> groupedRelationships.get(tag).orEmpty
        }
      }.toList
      i.combineAll
    }

  def setRelationshipMappings[T : NodeManifest](
    node: Node[T],
    mapping: Map[core.Relationship[_, _], Set[NodeWithRelationshipsOps.Relationship]])
    : XorT[GraphM, Error, Unit] = {
    val a = mapping
      .map {
        case (r: ToOne[_, _], relationships) =>
          setToOneRelationship(node, relationships, r)
        case (r: ToMany[_, _], relationships) =>
          node.toMany(relationships.map(_.id).toSet)(r)
        case (r: Tree[_], relationships) =>
          setTreeRelationships(node, relationships)
      }
    // TODO : investigate another approach
    // such as : implicit def plusInstance = XorT.eitherTPlus[GraphM, List[Error] Xor Unit]
    a.reduce { (a, b) => a.flatMap(_ => b) }
    }

  def setRelationships[T : NodeManifest](node: Node[T], relationships: Set[Relationship])
    : XorT[GraphM, List[Error], Unit] =
    for {
      legalRelationshipMapping <- XorT(GraphM(legalNodeRelationships[T].leftMap(List(_))))
      parsedRelationships <- XorT(GraphM(parseRelationships(relationships)))
      groupedRelationships = groupRelationships(parsedRelationships, legalRelationshipMapping)
      setRelationshipMapping <- setRelationshipMappings(node, groupedRelationships).leftMap(List(_))
    }
    yield {}

  def setToOneRelationship[T : NodeManifest](
    node: Node[T], ids: Set[NodeWithRelationshipsOps.Relationship], relationship: ToOne[_, _])
    : XorT[GraphM, Error, Unit] =
    ids.toList match {
      case i :: Nil => node.toOne(Some(i.id))(relationship)
      case Nil => node.toOne(Option.empty[Id])(relationship)
      case _ => XorT(GraphM(
        Error(s"Only one relationship allowed for ${relationship.uNodeManifest.tag}").left))
    }

  def setTreeRelationships[T : NodeManifest](
    node: Node[T], relationships: Set[NodeWithRelationshipsOps.Relationship])
    : XorT[GraphM, Error, Unit] = {
    val tree = TreeOps[T](node)

    val parentRelationship = relationships.find(_.label === Some(TreeOps.parentLabel))

    val childRelationships = relationships
      .filter(_.label === Some(TreeOps.childLabel))
      .toList

    (parentRelationship, childRelationships) match {
      case (None, Nil) => tree.parent(Option.empty[Id]).flatMap(_ => tree.children(Set.empty[Id]))
      case (None, _) => XorT(GraphM(Error(s"must be one ${TreeOps.parentLabel.v} relationship").left))
      case (_, Nil) => XorT(GraphM(Error(s"must be at least one ${TreeOps.childLabel.v} relationship").left))
      case (p, c) => tree.parent(p.map(_.id)).flatMap(_ => tree.children(c.map(_.id).toSet))
    }
  }

  def legalNodeRelationships[T : NodeManifest]: Error Xor Map[Tag, List[core.Relationship[_, _]]] = {
    relationshipMappings
      .get(NodeManifest[T].tag)
      .map { r => r.groupBy(_.uNodeManifest.tag) }
      .toRightXor(Error("Node has no relationships"))
    }

  def updateNodeWithRelationships[T : NodeManifest](nodeWithRelationships: NodeWithRelationships[T])
    : XorT[GraphM, List[Error], NodeWithRelationships[T]] =
    for {
      n <- updateNode(nodeWithRelationships.node).leftMap(List(_))
      v <- setRelationships(n, nodeWithRelationships.relationships)
    }
    yield nodeWithRelationships

}

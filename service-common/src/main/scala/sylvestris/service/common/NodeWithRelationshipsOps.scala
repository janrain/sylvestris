package sylvestris.service.common

import scalaz.{ \/, -\/, \/-, EitherT, ListT }
import scalaz.std.list._
import scalaz.std.map._
import scalaz.std.option._
import scalaz.std.set._
import scalaz.syntax.{ TreeOps => _, _ }, applicative._, either._, equal._, traverse._, std.list._, std.option._
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

  def splitNodePath(nodePath: String): Error \/ (Tag, Id) = {
    val invalidNodePath = Error(s"invalid node path $nodePath")
    nodePath match {
      case NodeWithRelationshipsOps.NodePathExtractor(pathSegment, id) =>
        pathSegmentToTag
          .get(PathSegment(pathSegment))
          .map(tag => (tag, Id(id)))
          .toRightDisjunction(invalidNodePath)
      case _ => invalidNodePath.left
    }
  }

  def edgeToRelationship(edge: Edge): Error \/ Relationship =
    tagToPathSegment
      .get(edge.tagB)
      .map(pathSegment => Relationship(edge.label.map(_.v), s"/api/${pathSegment.v}/${edge.idB.v}"))
      .toRightDisjunction(Error(s"path segment not found for ${edge.tagB}"))

  def nodeWithRelationships[T : NodeManifest](id: Id): EitherT[GraphM, Error, NodeWithRelationships[T]] =
    for {
      node <- getNode(id)
      edges <- getEdges(id, NodeManifest[T].tag)
      relationships <- EitherT(GraphM(edges.map(edgeToRelationship).toList.sequenceU))
    }
    yield NodeWithRelationships[T](node, relationships.toSet)

  def addNodeWithRelationships[T](nodeWithRelationships: NodeWithRelationships[T])(implicit nm: NodeManifest[T])
    : EitherT[GraphM, List[Error], NodeWithRelationships[T]] =
    for {
      n <- addNode(nodeWithRelationships.node).leftMap(_.point[List])
      v <- setRelationships(n, nodeWithRelationships.relationships)
    }
    yield nodeWithRelationships

  def parseRelationship(relationship: Relationship): Error \/ NodeWithRelationshipsOps.Relationship =
    splitNodePath(relationship.nodePath)
      .map {
        case (tag, id) => NodeWithRelationshipsOps.Relationship(id, tag, relationship.label.map(Label(_)))
      }

  def parseRelationships(relationships: Set[Relationship])
    : List[Error] \/ Set[NodeWithRelationshipsOps.Relationship] =
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
          relationship -> groupedRelationships.get(tag).orZero
        }
      }.toList
      i.suml
    }

  def setRelationshipMappings[T : NodeManifest](
    node: Node[T],
    mapping: Map[core.Relationship[_, _], Set[NodeWithRelationshipsOps.Relationship]])
    : EitherT[GraphM, Error, Unit] = {
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
    // such as : implicit def plusInstance = EitherT.eitherTPlus[GraphM, List[Error] \/ Unit]
    a.reduce { (a, b) => a.flatMap(_ => b) }
    }

  def setRelationships[T : NodeManifest](node: Node[T], relationships: Set[Relationship])
    : EitherT[GraphM, List[Error], Unit] =
    for {
      legalRelationshipMapping <- EitherT(GraphM(legalNodeRelationships[T].leftMap(List(_))))
      parsedRelationships <- EitherT(GraphM(parseRelationships(relationships)))
      groupedRelationships = groupRelationships(parsedRelationships, legalRelationshipMapping)
      setRelationshipMapping <- setRelationshipMappings(node, groupedRelationships).leftMap(List(_))
    }
    yield {}

  def setToOneRelationship[T : NodeManifest](
    node: Node[T], ids: Set[NodeWithRelationshipsOps.Relationship], relationship: ToOne[_, _])
    : EitherT[GraphM, Error, Unit] =
    ids.toList match {
      case i :: Nil => node.toOne(Some(i.id))(relationship)
      case Nil => node.toOne(Option.empty[Id])(relationship)
      case _ => EitherT(GraphM(
        Error(s"Only one relationship allowed for ${relationship.uNodeManifest.tag}").left))
    }

  def setTreeRelationships[T : NodeManifest](
    node: Node[T], relationships: Set[NodeWithRelationshipsOps.Relationship])
    : EitherT[GraphM, Error, Unit] = {
    val tree = TreeOps[T](node)

    val parentRelationship = relationships.find(_.label === Some(TreeOps.parentLabel))

    val childRelationships = relationships
      .filter(_.label === Some(TreeOps.childLabel))
      .toList
      .toNel

    (parentRelationship, childRelationships) match {
      case (None, None) => tree.parent(Option.empty[Id]).flatMap(_ => tree.children(Set.empty[Id]))
      case (None, _) => EitherT(GraphM(Error(s"must be one ${TreeOps.parentLabel.v} relationship").left))
      case (_, None) => EitherT(GraphM(Error(s"must be at least one ${TreeOps.childLabel.v} relationship").left))
      case (p, Some(c)) => tree.parent(p.map(_.id)).flatMap(_ => tree.children(c.map(_.id).toSet))
    }
  }

  def legalNodeRelationships[T : NodeManifest]: Error \/ Map[Tag, List[core.Relationship[_, _]]] = {
    relationshipMappings
      .get(NodeManifest[T].tag)
      .map { r => r.groupBy(_.uNodeManifest.tag) }
      .toRightDisjunction(Error("Node has no relationships"))
    }

  def updateNodeWithRelationships[T : NodeManifest](nodeWithRelationships: NodeWithRelationships[T])
    : EitherT[GraphM, List[Error], NodeWithRelationships[T]] =
    for {
      n <- updateNode(nodeWithRelationships.node).leftMap(_.point[List])
      v <- setRelationships(n, nodeWithRelationships.relationships)
    }
    yield nodeWithRelationships

}

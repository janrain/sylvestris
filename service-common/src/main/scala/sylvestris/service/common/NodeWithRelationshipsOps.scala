package sylvestris.service.common

import scalaz.{ \/, -\/, \/-, EitherT }
import scalaz.std.list._
import scalaz.syntax._, either._, traverse._, std.option._
import sylvestris._, core._, GraphM._

case class NodeWithRelationshipsOps(
  relationshipMappings: Map[Tag, List[core.Relationship[_, _]]],
  pathSegmentToTag: Map[PathSegment[_], Tag]) {

  val NodePathExtractor = "/api/(.*?)/(.*)".r

  def splitNodePath(nodePath: String): Error \/ (Tag, Id) = {
    val invalidNodePath = Error(s"invalid node path $nodePath")
    nodePath match {
      case NodePathExtractor(pathSegment, id) =>
        pathSegmentToTag
          .get(PathSegment(pathSegment))
          .map(tag => (tag, Id(id)))
          .toRightDisjunction(invalidNodePath)
      case _ => invalidNodePath.left
    }
  }

  def nodeWithRelationships[T : NodeManifest](id: Id) = {
    for {
      n <- getNode(id)
      e <- getEdges(id, NodeManifest[T].tag)
    }
    yield NodeWithRelationships[T](n, e.map(e => Relationship(s"/api/${e.tagB.v}/${e.idB.v}")))
  }.run

  def addNodeWithRelationships[T](nodeWithRelationships: NodeWithRelationships[T])(implicit nm: NodeManifest[T])
    : GraphM[List[Error] \/ NodeWithRelationships[T]] = {
      for {
        n <- addNode(nodeWithRelationships.node).leftMap(List(_))
        v <- EitherT(setRelationships(n, nodeWithRelationships.relationships))
      }
      yield nodeWithRelationships
    }.run

  def setRelationships[T : NodeManifest](node: Node[T], relationships: Set[Relationship])
    : GraphM[List[Error] \/ Unit] = {
    val x: List[Error \/ (Tag, Id)] = relationships.toList.map(r => splitNodePath(r.nodePath))
    val y: List[Error] \/ List[(Tag, Id)] = x.traverseU(_.leftMap(List(_)))
    val z: List[Error] \/ Map[Tag, List[Id]] = y.map { m =>
      m.groupBy { case (tag, _) => tag } mapValues(_.map { case (_, id) => id })
    }

    (z, availableNodeRelationships[T].leftMap(List(_))) match {
      case (\/-(idsByTag), \/-(availableRelationships)) =>
        val a: List[GraphM[List[Error] \/ Unit]] = availableRelationships.map {
          case r: ToOne[_, _] => setToOneRelationship(node, idsByTag.get(r.uNodeManifest.tag), r).leftMap(List(_)).run
          case _ => ???
        }
        sequence(a.map(EitherT(_))).map(_ => ()).run
      case (-\/(i), -\/(j)) => GraphM((i ++ j).left)
      case (-\/(i), _) => GraphM(i.left)
      case (_, -\/(j)) => GraphM(j.left)
    }
//    val a: GraphM[Validation[List[Error], Unit]] = availableNodeRelationships[T].map(_.map {
//      case r: ToOne[_, _] =>
//        val zmap: Validation[List[Error], GraphM[Error \/ Unit]] = z.map(m => setToOneRelationship(node, m.get(r.uNodeManifest.tag), r))
//
//        // zmap: Validation[List[Error], GraphM[Validation[List[Error], Unit]]]
//        //
//
//        // GraphM[Validation[List[Error], Unit]]
//        zmap
//
//      //case r: ToMany[_, _] =>
//      //  z.map(m => node.toMany(m.get(r.uNodeManifest.tag).toList.flatten.toSet)(r).right)
//      //case r: core.Tree[_] =>
//      //  z.map(_.get(r.uNodeManifest.tag) match {
//      //    // TODO We need the labels for this
//      //    case _ => Error("bloop").left
//      //  })
//      //case r: core.Relationship[_, _] =>
//      //// this seems like it'd be undefined?
//      //// How can we make it so we don't need to cover this option?
//      //  z.map(_.get(r.uNodeManifest.tag) match {
//      //    case _ => Error("bloop").left
//      //  })
//    })
  }

  def setToOneRelationship[T : NodeManifest](node: Node[T], ids: Option[List[Id]], relationship: ToOne[_, _])
    : EitherT[GraphM, Error, Unit] =
    ids match {
      case Some(h :: Nil)   => node.toOne(Some(h))(relationship)
      case None | Some(Nil) => node.toOne(Option.empty[Id])(relationship)
      case _                =>
        EitherT(GraphM(Error(s"Only one relationship allowed for ${relationship.uNodeManifest.tag}").left))
    }

  def availableNodeRelationships[T : NodeManifest]: Error \/ List[core.Relationship[_, _]] =
    relationshipMappings
      .get(NodeManifest[T].tag)
      .toRightDisjunction(Error("Node has no relationships"))

    /*
     *
     * [
     *   { nodePath: orgs/org1 },
     *   { nodePath: orgs/org2 },
     *   { nodePath: customers/cust1 }
     * ]
     *
     * toMany(Set(org1, org2))
     */

  def updateNodeWithRelationships[T : NodeManifest](nodeWithRelationships: NodeWithRelationships[T])
    : EitherT[GraphM, Error, NodeWithRelationships[T]] =
    for {
      n <- updateNode(nodeWithRelationships.node)
    }
    yield nodeWithRelationships

}

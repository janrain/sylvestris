package sylvestris.service.common

import scalaz.{ Id => _, Node => _, _ }, Scalaz.{ Id => _,  _ }
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

  def nodeWithRelationships[T : NodeManifest](id: Id) =
    for {
      n <- getNode(id)
      e <- getEdges(id, NodeManifest[T].tag)
    }
    yield n.map(v => NodeWithRelationships[T](v, e.map(e => Relationship(s"/api/${e.tagB.v}/${e.idB.v}"))))

  def addNodeWithRelationships[T](nodeWithRelationships: NodeWithRelationships[T])(implicit nm: NodeManifest[T])
    : GraphM[Validation[List[Error], NodeWithRelationships[T]]]=
    for {
      n <- addNode(nodeWithRelationships.node)
      v <- setRelationships(n, nodeWithRelationships.relationships)
    }
    yield v.map(_ => nodeWithRelationships)

  def setRelationships[T : NodeManifest](node: Node[T], relationships: Set[Relationship])
    : GraphM[Validation[List[Error], Unit]] = {
    val x: List[Error \/ (Tag, Id)] = relationships.toList.map(r => splitNodePath(r.nodePath))
    val y: Validation[List[Error], List[(Tag, Id)]] = x.traverseU(_.leftMap(List(_)).validation)
    val z: Validation[List[Error], Map[Tag, List[Id]]] = y.map { m =>
      m.groupBy { case (tag, _) => tag } mapValues(_.map { case (_, id) => id })
    }

    (z, availableNodeRelationships[T].leftMap(List(_)).validation) match {
      case (Success(idsByTag), Success(availableRelationships)) =>
        val a: List[GraphM[Validation[List[Error], Unit]]] = availableRelationships.map {
          case r: ToOne[_, _] =>
            val monkey: GraphM[Error \/ Unit] = setToOneRelationship(node, idsByTag.get(r.uNodeManifest.tag), r)
            monkey.map(_.leftMap(List(_)).validation)
          case _ => ???
        }
        val b: GraphM[Iterable[Validation[List[Error], Unit]]] = sequence(a)
        val c: GraphM[List[Validation[List[Error], Unit]]] = b.map(_.toList)
        val d: GraphM[Validation[List[Error], Unit]] = c.map(_.suml)
        d
      case (Failure(i), Failure(j)) => GraphM(Failure(i ++ j))
      case (Failure(i), _) => GraphM(Failure(i))
      case (_, Failure(j)) => GraphM(Failure(j))
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
    : GraphM[Error \/ Unit] = {
    val x: Error \/ GraphM[Unit] = ids match {
      case Some(h :: Nil)   => node.toOne(Some(h))(relationship).right
      case None | Some(Nil) => node.toOne(Option.empty[Id])(relationship).right
      case _                => Error(s"Only one relationship allowed for ${relationship.uNodeManifest.tag}").left
    }

    // TODO sequence needs more scalaz
    //sequence(x)
    GraphM(g => x.map(_.run(g)))
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

  def updateNodeWithRelationships[T : NodeManifest](nodeWithRelationships: NodeWithRelationships[T]) =
    for {
      n <- updateNode(nodeWithRelationships.node)
    }
    yield nodeWithRelationships

}

package sylvestris.service.common

import scalaz.syntax.equal._
import sylvestris._, core._, GraphM._

case class NodeWithRelationshipsOps(
  relationshipMappings: Map[Tag, List[core.Relationship[_, _]]],
  pathSegmentToTag: Map[PathSegment[_], Tag]) {

  val NodePathExtractor = "/api/(.*?)/(.*)".r

  def splitNodePath(nodePath: String): (Tag, Id) = {
    def invalidNodePath() = sys.error(s"invalid node path $nodePath")
    nodePath match {
      case NodePathExtractor(pathSegment, id) =>
        (pathSegmentToTag.getOrElse(PathSegment(pathSegment), invalidNodePath()), Id(id))
      case _ => invalidNodePath()
    }
  }

  def nodeWithRelationships[T : NodeManifest](id: Id) =
    for {
      n <- getNode(id)
      e <- getEdges(id, NodeManifest[T].tag)
    }
    yield n.map(v => NodeWithRelationships[T](v, e.map(e => Relationship(s"/api/${e.tagB.v}/${e.idB.v}"))))


    // TODO expand this for other OneToMany/ManyToOne
    // def link(gedges: Set[GEdge])
    //   (idA: String, tagA: String, idB: String, tagB: String)
    //   (relationships: List[Relationship[_, _]])
    //   : GraphM[Graph] = GraphM { g =>
    //   val relationship: Relationship[_, _] = relationships.find(_.uTag.v === tagB) match {
    //     case Some(r : OneToOne[_, _])     =>
    //       g.removeEdges(idA, tagA, tagB)
    //       // for all x nodes b links to, remove edges from x to tagB
    //       g.getEdges(idB, tagB, tagA).foreach(e => g.removeEdges(e.idB, tagA, tagB))
    //       g.removeEdges(idB, tagB, tagA)
    //       r
    //     case Some(r : Relationship[_, _]) => r
    //     case None => sys.error(s"no relationship between $tagA and $tagB")
    //   }
    //   g.addEdge(GEdge(relationship.labelTU.map(_.v), idA, tagA, idB, tagB))
    //   g.addEdge(GEdge(relationship.labelUT.map(_.v), idB, tagB, idA, tagA))
    // }


  def addNodeWithRelationships[T](nodeWithRelationships: NodeWithRelationships[T])(implicit nm: NodeManifest[T]) = {
    // TODO : move sys errors to specific return type
    for {
      n <- addNode(nodeWithRelationships.node)
      _ <- sequence(nodeWithRelationships.relationships.map { relationship =>
        val (tag, id) = splitNodePath(relationship.nodePath)
        val relationships: List[core.Relationship[_, _]] = relationshipMappings
          .getOrElse(nm.tag, sys.error("Node has no relationships"))
        // TODO expand this for other OneToMany/Tree
        relationships.find(_.uNodeManifest.tag === tag) match {
          case Some(r : ToOne[_, _]) => n.toOne(Some(id))(r)
          case Some(_ : core.Relationship[_, _]) => GraphM(())
          case None => sys.error(s"no relationship between ${nm.tag} and $tag")
        }
      })
    }
    yield nodeWithRelationships
  }

  def updateNodeWithRelationships[T : NodeManifest](nodeWithRelationships: NodeWithRelationships[T]) =
    for {
      n <- updateNode(nodeWithRelationships.node)
    }
    yield nodeWithRelationships

}

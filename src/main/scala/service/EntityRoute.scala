package service

import graph._, GraphM._
import scalaz.syntax.equal._
import spray.httpx.SprayJsonSupport._
import spray.json._, DefaultJsonProtocol._
import spray.routing._, HttpService._

/*
  {
    node : {}
    relationships : [
      {
        "label" : <optional_label>,
        "node" : <path>,
      },
      ...
    ]
  }
*/

case class NodeWithRelationships[T](node: Node[T], relationships: Set[Relationship])

case class Relationship(nodePath: String)

object Relationship {
  implicit val jsonFormat = jsonFormat1(apply)
}

object NodeWithRelationships {
  import spray.json.DefaultJsonProtocol._

  implicit def jsonFormat[T : NodeManifest] = new RootJsonFormat[NodeWithRelationships[T]] {
    def write(n: NodeWithRelationships[T]) = JsObject(
      "id" -> JsString(n.node.id.v),
      "content" -> n.node.content.toJson(NodeManifest[T].jsonFormat),
      "relationships" -> n.relationships.toJson)

    def read(value: JsValue) = value match {
      case v: JsObject => v.getFields("id", "content", "relationships") match {
        case Seq(JsString(id), content, relationships) =>
          NodeWithRelationships(
            Node(Id(id), content.convertTo[T](NodeManifest[T].jsonFormat)),
            relationships.convertTo[Set[Relationship]])
      }
      case _ => deserializationError("id expected")
    }
  }
}

case class NodeWithRelationshipsOps(relationshipMappings: Map[Tag, List[graph.Relationship[_, _]]]) {

  val NodePathExtractor = "/api/(.*?)/(.*)".r

  def splitNodePath(nodePath: String): (Tag, Id) = {
    def invalidEntityPath() = sys.error(s"invalid node path $nodePath")
    nodePath match {
      case NodePathExtractor(pathSegment, id) =>
        (NodeRoute.pathSegmentToTag.getOrElse(PathSegment(pathSegment), invalidEntityPath()), Id(id))
      case _ => invalidEntityPath()
    }
  }

  def nodeWithRelationships[T : NodeManifest](id: Id) =
    for {
      n <- lookupNode(id)
      e <- lookupEdges(id, NodeManifest[T].tag)
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
    //       g.lookupEdges(idB, tagB, tagA).foreach(e => g.removeEdges(e.idB, tagA, tagB))
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
        val relationships: List[graph.Relationship[_, _]] = relationshipMappings
          .getOrElse(nm.tag, sys.error("Node has no relationships"))
        // TODO expand this for other OneToMany/ManyToOne
        val op: GraphM[Unit] = relationships.find(_.uNodeManifest.tag === tag) match {
          case Some(r : OneToOne[_, _]) =>
            for {
              _ <- removeEdges(n.id, nm.tag, tag)
              // for all x nodes b links to, remove edges from x to tagB
              e <- lookupEdges(id, tag, nm.tag)
              _ <- removeEdges(e)
              _ <- removeEdges(id, tag, nm.tag)
            } yield {}
          case Some(_ : graph.Relationship[_, _]) => GraphM(())
          case None => sys.error(s"no relationship between ${nm.tag} and $tag")
        }
        for {
          _ <- op
          // TODO : add Label to Relationship
          _ <- addEdges(Set(Edge(None, n.id, nm.tag, id, tag)))
        } yield {}
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

// TOOD relationshipMappings should be injected in some nicer way
case class EntityRoute[T]
  (relationshipMappings: Map[Tag, List[graph.Relationship[_, _]]])
  (implicit nm: NodeManifest[T], val pathSegment: PathSegment[T])  {

  import nm.jsonFormat

  val tag = nm.tag

  val tableOfContents =
    complete {
      nodes().run(InMemoryGraph).map(_.id.v)
    }

  val nodeOps = NodeWithRelationshipsOps(relationshipMappings)

  // TODO : constrain relationships
  val create = entity(as[NodeWithRelationships[T]]) { nodeWithRelationships =>
    complete {
      nodeOps.addNodeWithRelationships[T](nodeWithRelationships).run(InMemoryGraph)
    }
  }

  def read(id: Id) =
    complete {
      nodeOps.nodeWithRelationships(id).run(InMemoryGraph)
    }

    // TODO : constrain relationships
  def update(id: Id) = entity(as[NodeWithRelationships[T]]) { nodeWithRelationships =>
    complete {
      if (nodeWithRelationships.node.id =/= id) {
        sys.error("id mismatch - view and URL id must match")
      }
      nodeOps.updateNodeWithRelationships[T](nodeWithRelationships).run(InMemoryGraph)
    }
  }

  def delete(id: Id) =
    complete {
      removeNode(id).run(InMemoryGraph)
      Map("status" -> "deleted")
    }

  val crudRoute =
    pathPrefix(pathSegment.v)(
      pathEnd(
        get(tableOfContents) ~
        post(create)) ~
      path(idMatcher)(id =>
        get(read(id)) ~
        put(update(id)) ~
        HttpService.delete(delete(id))
      )
    )

}

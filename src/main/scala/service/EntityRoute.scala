package service

import graph._, GraphM._
import scalaz.syntax.equal._
import spray.httpx.SprayJsonSupport._
import spray.json._, DefaultJsonProtocol._
import spray.routing._, HttpService._

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

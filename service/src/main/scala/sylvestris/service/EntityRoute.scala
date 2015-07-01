package sylvestris.service

import sylvestris._, core._, GraphM._
import scalaz.syntax.equal._
import spray.httpx.SprayJsonSupport._
import spray.json._, DefaultJsonProtocol._
import spray.routing._, HttpService._
import sylvestris.service.common._

// TODO: rename to NodeRoute
case class NodeRoute[T]()
  (implicit nm: NodeManifest[T], val pathSegment: PathSegment[T])  {

  import nm.jsonFormat

  val tag = nm.tag

  val tableOfContents =
    complete {
      nodes().run(InMemoryGraph).map(_.id.v)
    }

  // TODO : constrain relationships
  def create(nodeWithRelationshipsOps: NodeWithRelationshipsOps) =
    entity(as[NodeWithRelationships[T]]) { nodeWithRelationships =>
      complete {
        nodeWithRelationshipsOps.addNodeWithRelationships[T](nodeWithRelationships).run(InMemoryGraph)
      }
    }

  def read(id: Id, nodeWithRelationshipsOps: NodeWithRelationshipsOps) =
    complete {
      nodeWithRelationshipsOps.nodeWithRelationships(id).run(InMemoryGraph)
    }

    // TODO : constrain relationships
  def update(id: Id, nodeWithRelationshipsOps: NodeWithRelationshipsOps) =
    entity(as[NodeWithRelationships[T]]) { nodeWithRelationships =>
      complete {
        if (nodeWithRelationships.node.id =/= id) {
          sys.error("id mismatch - view and URL id must match")
        }
        nodeWithRelationshipsOps.updateNodeWithRelationships[T](nodeWithRelationships).run(InMemoryGraph)
      }
    }

  def delete(id: Id) =
    complete {
      removeNode(id).run(InMemoryGraph)
      Map("status" -> "deleted")
    }

  def crudRoute(nodeWithRelationshipsOps: NodeWithRelationshipsOps) =
    pathPrefix(pathSegment.v)(
      pathEnd(
        get(tableOfContents) ~
        post(create(nodeWithRelationshipsOps))) ~
      path(idMatcher)(id =>
        get(read(id, nodeWithRelationshipsOps)) ~
        put(update(id, nodeWithRelationshipsOps)) ~
        HttpService.delete(delete(id))
      )
    )

}

package sylvestris.service

import scalaz.\/
import scalaz.syntax.equal._
import spray.httpx.SprayJsonSupport._
import spray.json._, DefaultJsonProtocol._
import spray.routing._, HttpService._
import sylvestris._, core._, GraphM._, service.common._

// TODO : find home
object disjunctionWriter {
  implicit def jsonFormatter[T : JsonFormat, U : JsonFormat] = new RootJsonWriter[T \/ U] {
    def write(v: T \/ U) = v.fold(_.toJson, _.toJson)
  }
}

case class NodeRoute[T](graph: Graph)
  (implicit nm: NodeManifest[T], val pathSegment: PathSegment[T])  {

  import nm.jsonFormat
  import disjunctionWriter._

  val tag = nm.tag

  val tableOfContents =
    complete {
      nodes().run.run(graph).map(_.map(_.id.v))
    }

  def create(nodeWithRelationshipsOps: NodeWithRelationshipsOps) =
    entity(as[NodeWithRelationships[T]]) { nodeWithRelationships =>
      complete {
        nodeWithRelationshipsOps.addNodeWithRelationships[T](nodeWithRelationships).run(graph)
      }
    }

  def read(id: Id, nodeWithRelationshipsOps: NodeWithRelationshipsOps) =
    complete {
      nodeWithRelationshipsOps.nodeWithRelationships(id).run(graph)
    }

  def update(id: Id, nodeWithRelationshipsOps: NodeWithRelationshipsOps) =
    entity(as[NodeWithRelationships[T]]) { nodeWithRelationships =>
      complete {
        if (nodeWithRelationships.node.id =/= id) {
          sys.error("id mismatch - view and URL id must match")
        }
        nodeWithRelationshipsOps.updateNodeWithRelationships[T](nodeWithRelationships).run.run(graph)
      }
    }

  def delete(id: Id) =
    complete {
      removeNode(id).run.run(graph)
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

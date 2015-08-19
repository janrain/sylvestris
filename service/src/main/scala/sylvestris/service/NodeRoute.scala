package sylvestris.service

import scalaz.{ \/, EitherT }
import scalaz.syntax.equal._
import spray.http.StatusCodes.BadRequest
import spray.httpx.marshalling._
import spray.httpx.SprayJsonSupport._
import spray.json._, DefaultJsonProtocol._
import spray.routing._, HttpService._
import sylvestris._, core._, Graph._, service.common._

case class NodeRoute[T](graph: Graph)
  (implicit nm: NodeManifest[T], val pathSegment: PathSegment[T])  {

  import nm.jsonFormat

  val tag = nm.tag

  val tableOfContents =
    nodes().run.run(graph).fold(
      i => respondWithStatus(BadRequest)(complete(i)),
      i => complete(i.map(_.id.v)))

  // TODO : be more precise with error status codes

  def handleDisjunction(v: ToResponseMarshallable \/ ToResponseMarshallable) =
    v.fold(i => respondWithStatus(BadRequest)(complete(i)), complete(_))

  def handle[U : ToResponseMarshaller, V : ToResponseMarshaller](
    nodeWithRelationshipsOps: NodeWithRelationshipsOps,
    f: NodeWithRelationshipsOps => EitherT[GraphM, U, V]) =
    f(nodeWithRelationshipsOps).run.run(graph)
      .fold(i => respondWithStatus(BadRequest)(complete(i)), complete(_))

  def create(nodeWithRelationshipsOps: NodeWithRelationshipsOps) =
    entity(as[NodeWithRelationships[T]]) { nodeWithRelationships =>
      handle(nodeWithRelationshipsOps, _.addNodeWithRelationships[T](nodeWithRelationships))
    }

  def read(id: Id, nodeWithRelationshipsOps: NodeWithRelationshipsOps) =
    handle(nodeWithRelationshipsOps, _.nodeWithRelationships(id))

  def update(id: Id, nodeWithRelationshipsOps: NodeWithRelationshipsOps) =
    entity(as[NodeWithRelationships[T]]) { nodeWithRelationships =>
      if (nodeWithRelationships.node.id =/= id) {
        respondWithStatus(BadRequest)(complete("id mismatch - view and URL id must match"))
      }
      else {
        handle(nodeWithRelationshipsOps, _.updateNodeWithRelationships[T](nodeWithRelationships))
      }
    }

  def remove(id: Id) = complete {
    removeNode(id).run.run(graph).fold(BadRequest -> _, _ => Map("status" -> "deleted"))
  }

  def crudRoute(nodeWithRelationshipsOps: NodeWithRelationshipsOps) =
    pathPrefix(pathSegment.v) {
      pathEnd(
        get(tableOfContents) ~
        post(create(nodeWithRelationshipsOps))) ~
      path(idMatcher) { id =>
        get(read(id, nodeWithRelationshipsOps)) ~
        put(update(id, nodeWithRelationshipsOps)) ~
        delete(remove(id))
      }
    }

}

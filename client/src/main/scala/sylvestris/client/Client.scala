package sylvestris.client

import akka.actor._
import sylvestris.core._
import scala.concurrent._
import spray.client.pipelining._
import spray.http._, Uri._
import spray.httpx._, marshalling._, unmarshalling._
import sylvestris.service.common._

object Client {
  def apply(host: Uri.Host)(implicit system: ActorSystem): Client = Client(host, 80, "http")
}

case class Client(host: Uri.Host, port: Int, scheme: String)(implicit system: ActorSystem) {

  import system.dispatcher

  def path(v: Path): Uri = Uri(
    scheme = scheme,
    authority = Uri.Authority(host = host, port = port),
    path = Path("/api/") ++ v)

  def pipeline[T : λ[U => FromResponseUnmarshaller[NodeWithRelationships[U]]]]
    : HttpRequest => Future[NodeWithRelationships[T]] =
    sendReceive ~> unmarshal[NodeWithRelationships[T]]

  def get[T
    : PathSegment
    : λ[U => FromResponseUnmarshaller[NodeWithRelationships[U]]]]
    (id: Id)
    : Future[NodeWithRelationships[T]] = {
    pipeline.apply(Get(path(Path(s"${PathSegment[T].v}/${id.v}"))))
  }

  def put[T
    : PathSegment
    : λ[U => FromResponseUnmarshaller[NodeWithRelationships[U]]]
    : λ[U => Marshaller[NodeWithRelationships[U]]]]
    (v: NodeWithRelationships[T])
    : Future[NodeWithRelationships[T]] = {
    pipeline.apply(Put(path(Path(s"${PathSegment[T].v}/${v.node.id.v}")), v))
  }

  def post[T
    : PathSegment
    : λ[U => FromResponseUnmarshaller[NodeWithRelationships[U]]]
    : λ[U => Marshaller[NodeWithRelationships[U]]]]
    (v: NodeWithRelationships[T])
    : Future[NodeWithRelationships[T]] = {
    pipeline.apply(Post(path(Path(PathSegment[T].v)), v))
  }

  def delete[T
    : PathSegment
    : λ[U => FromResponseUnmarshaller[NodeWithRelationships[U]]]]
    (id: Id)
    : Future[NodeWithRelationships[T]] = {
    pipeline.apply(Delete(path(Path(s"${PathSegment[T].v}/${id.v}"))))
  }

}

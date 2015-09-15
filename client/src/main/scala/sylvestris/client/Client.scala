package sylvestris.client

import akka.actor._
import sylvestris.core._
import scala.concurrent._
import spray.client.pipelining._
import spray.http._, Uri._
import spray.httpx._, unmarshalling._
import sylvestris.service.common._

case class Client(host: Uri.Host, port: Int = 80, scheme: String = "http")(
  implicit system: ActorSystem) {

  import system.dispatcher

  def path(v: Path): Uri = Uri(
    scheme = scheme,
    authority = Uri.Authority(host = host, port = port),
    path = Path("/api/") ++ v)

  def get[T : PathSegment : λ[U => FromResponseUnmarshaller[NodeWithRelationships[U]]]]
    (id: Id): Future[NodeWithRelationships[T]] = {
    val pipeline: HttpRequest => Future[NodeWithRelationships[T]] =
      sendReceive ~> unmarshal[NodeWithRelationships[T]]

    pipeline(
      Get(path(Path(s"${PathSegment[T].v}/${id.v}"))))
  }
}

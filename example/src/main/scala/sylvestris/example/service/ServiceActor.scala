package sylvestris.example.service

import akka.actor.Actor
import cats.data.Xor
import shapeless.HNil
import spray.httpx.SprayJsonSupport._
import spray.json._
import spray.routing.directives.ExecutionDirectives._
import spray.routing._
import sylvestris._, core._, service._, common._, example.model._

// TODO : find home
object disjunctionWriter {
  implicit def jsonFormatter[T : JsonFormat, U : JsonFormat] = new RootJsonWriter[T Xor U] {
    def write(v: T Xor U) = v.fold(_.toJson, _.toJson)
  }
}

object HandleExceptions extends Directive0 {
  def happly(f: HNil => Route): Route = handleExceptions(handler)(f(HNil))

  private val handler = ExceptionHandler {
    case ex: Exception => ctx =>
      ctx.complete(ex.getMessage)
  }
}

class ServiceActor(nodeRoutes: List[NodeRoute[_]], nodeWithRelationshipsOps: NodeWithRelationshipsOps, graph: Graph)
  extends Actor with HttpService with Directives {

  import disjunctionWriter._

  implicit lazy val actorRefFactory = context

  val receive = runRoute(route)

  lazy val route = HandleExceptions {
    pathPrefix("api") {
      nodeRoutes.map(_.crudRoute(nodeWithRelationshipsOps)).reduce(_ ~ _) ~
      // TODO clean this up
      pathPrefix("org_cust_view")(
        path(idMatcher)(id =>
          get(
            complete(CustomDataView.get(id).value.run(graph))) ~
          put(
            entity(as[CustomData]) { data =>
              complete(CustomDataView.update(id, data).value.run(graph))
            })))
    }
  }

}

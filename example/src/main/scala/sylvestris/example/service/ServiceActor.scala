package sylvestris.example.service

import akka.actor.{ Actor, ActorSystem, Props }
import sylvestris._, core._, service._, common._, example.model._
import spray.httpx.SprayJsonSupport._
import spray.routing.directives.ExecutionDirectives._
import spray.routing._
import shapeless.HNil

object HandleExceptions extends Directive0 {
  def happly(f: HNil => Route): Route = handleExceptions(handler)(f(HNil))

  private val handler = ExceptionHandler {
    case ex: Exception => ctx =>
      ctx.complete(ex.getMessage)
  }
}

class ServiceActor(nodeRoutes: List[EntityRoute[_]], nodeWithRelationshipsOps: NodeWithRelationshipsOps)
  extends Actor with HttpService with Directives {

  implicit lazy val actorRefFactory = context

  val receive = runRoute(route)

  lazy val route = HandleExceptions {
    pathPrefix("api") {
      nodeRoutes.map(_.crudRoute(nodeWithRelationshipsOps)).reduce(_ ~ _) ~
      // TODO clean this up
      pathPrefix("org_cust_lens")(
        path(idMatcher)(id =>
          get(
            complete(CustomLens.get(id).run(InMemoryGraph))) ~
          put(
            entity(as[CustomData]) { data =>
              complete(CustomLens.update(id, data).run(InMemoryGraph))
            })))
    }
  }

}

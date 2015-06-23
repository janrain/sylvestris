package service

import akka.actor.{ Actor, ActorSystem, Props }
import graph._
import spray.httpx.SprayJsonSupport._
import spray.routing.directives.ExecutionDirectives._
import model._
import spray.routing._
import shapeless.HNil

object NodeRoute {
  object pathSegments {
    implicit val customer = PathSegment[Customer]("customers")
    implicit val organization = PathSegment[Organization]("orgs")
  }

  import pathSegments._

  val nodeRoutes = List(
    EntityRoute[Customer](relationships.relationshipMappings),
    EntityRoute[Organization](relationships.relationshipMappings))

  val pathSegmentToTag = nodeRoutes.map(i => i.pathSegment.v -> i.tag.v).toMap
}

object HandleExceptions extends Directive0 {
  def happly(f: HNil => Route): Route = handleExceptions(handler)(f(HNil))

  private val handler = ExceptionHandler {
    case ex: Exception => ctx =>
      ctx.complete(ex.getMessage)
  }
}

class ServiceActor extends Actor with HttpService with Directives {

  implicit lazy val actorRefFactory = context

  val receive = runRoute(route)

  lazy val route = HandleExceptions {
    pathPrefix("api") {
      NodeRoute.nodeRoutes.map(_.crudRoute).reduce(_ ~ _) ~
      // TODO clean this up
      pathPrefix("org_cust_lens")(
        path(new IdMatcher[Organization])(id =>
          get(
            complete(CustomLens.get(id).run(InMemoryGraph))) ~
          put(
            entity(as[CustomData]) { data =>
              complete(CustomLens.update(id, data).run(InMemoryGraph))
            })))
    }
  }

}

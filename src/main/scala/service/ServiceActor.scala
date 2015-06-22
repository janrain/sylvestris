package service

import akka.actor.{ Actor, ActorSystem, Props }
import graph._
import spray.httpx.SprayJsonSupport._
import model._
import spray.routing._

object NodeRoute {
  val nodeRoutes = List(
    EntityRoute[Customer]("customers"),
    EntityRoute[Organization]("orgs"))

  val pathSegmentToTag = nodeRoutes.map(i => i.pathSegment -> i.tag.v).toMap
}

class ServiceActor extends Actor with HttpService with Directives {

  implicit lazy val actorRefFactory = context

  val receive = runRoute(route)

  lazy val route =
    pathPrefix("api") {
      NodeRoute.nodeRoutes.map(_.crudRoute).reduce(_ ~ _)
      EntityRoute[Customer]("customers").crudRoute ~
      EntityRoute[Organization](PathSegment[Organization].v).crudRoute ~
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

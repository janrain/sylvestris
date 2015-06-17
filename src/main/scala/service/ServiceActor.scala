package service

import akka.actor.{ Actor, ActorSystem, Props }
import graph._
import spray.httpx.SprayJsonSupport._
import model._
import spray.routing._

class ServiceActor extends Actor with HttpService with Directives {

  implicit lazy val actorRefFactory = context

  val receive = runRoute(route)

  lazy val route =
    pathPrefix("api") {
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

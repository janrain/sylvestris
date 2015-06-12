package service

import akka.actor.{ Actor, ActorSystem, Props }
import model._
import spray.routing._
import spray.httpx.marshalling.ToResponseMarshallable

class ServiceActor extends Actor with HttpService with Directives {

  implicit lazy val actorRefFactory = context

  val receive = runRoute(route)

  lazy val route =
    pathPrefix("api") {
      EntityRoute[Customer]("customers").crudRoute ~
      EntityRoute[Organization]("orgs").crudRoute
    }

}

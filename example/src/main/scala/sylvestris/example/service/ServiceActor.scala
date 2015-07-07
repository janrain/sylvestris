package sylvestris.example.service

import akka.actor.{ Actor, ActorSystem, Props }
import sylvestris._, core._, service._, common._, example.model._
import spray.httpx.SprayJsonSupport._
import spray.routing.directives.ExecutionDirectives._
import spray.routing._
import shapeless.HNil

object NodeRoutes {

  object pathSegments {
    implicit val customer = PathSegment[Customer]("customers")
    implicit val organization = PathSegment[Organization]("orgs")
  }

  import pathSegments._

  val nodeRoutes: List[NodeRoute[_]] =
    List(
      NodeRoute[Customer] _,
      NodeRoute[Organization] _)
      .map(_(InMemoryGraph))

  val pathSegmentToTag: Map[PathSegment[_], Tag] = nodeRoutes
      .map(i => i.pathSegment -> i.tag)
      .toMap

  val klass = getClass

  // TODO : stringly package
  val nodeWithRelationshipsOps: NodeWithRelationshipsOps =
    NodeWithRelationshipsOps(RelationshipMappings("sylvestris.example").mapping, pathSegmentToTag)
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
      pathPrefix("org_cust_lens")(
        path(idMatcher)(id =>
          get(
            complete(CustomLens.get(id).run.run(graph))) ~
          put(
            entity(as[CustomData]) { data =>
              complete(CustomLens.update(id, data).run.run(graph))
            })))
    }
  }

}

package sylvestris.example.service

import sylvestris.core._
import sylvestris.example.model._
import sylvestris.service._, common._

object NodeRoute {

  object pathSegments {
    implicit val customer = PathSegment[Customer]("customers")
    implicit val organization = PathSegment[Organization]("orgs")
  }

  import pathSegments._

  val nodeRoutes: List[EntityRoute[_]] = List(
    EntityRoute[Customer](),
    EntityRoute[Organization]())

  val pathSegmentToTag: Map[PathSegment[_], Tag] = nodeRoutes
      .map(i => i.pathSegment -> i.tag)
      .toMap

  val klass = getClass

  // TODO : stringly package
  val nodeWithRelationshipsOps: NodeWithRelationshipsOps =
    NodeWithRelationshipsOps(RelationshipMappings("sylvestris.example").mapping, pathSegmentToTag)

}

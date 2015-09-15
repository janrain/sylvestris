package sylvestris.example.service

import sylvestris._, core._, service._, common._, example.model._

object NodeRoutes {
  object pathSegments {
    implicit val customer = PathSegment[Customer]("customers")
    implicit val organization = PathSegment[Organization]("orgs")
  }
}

case class NodeRoutes(graph: Graph) {
  import NodeRoutes.pathSegments._

  val routes: List[NodeRoute[_]] =
    List(
      NodeRoute[Customer] _,
      NodeRoute[Organization] _)
      .map(_(graph))

  val pathSegmentToTag: Map[PathSegment[_], Tag] = routes
      .map(i => i.pathSegment -> i.tag)
      .toMap

  val tagToPathSegment: Map[Tag, PathSegment[_]] = routes
      .map(i => i.tag -> i.pathSegment)
      .toMap

  val examplePackage = getClass.getPackage.getName.split('.').dropRight(1).mkString(".")

  val nodeWithRelationshipsOps: NodeWithRelationshipsOps =
    NodeWithRelationshipsOps(RelationshipMappings(examplePackage).mapping, pathSegmentToTag, tagToPathSegment)
}

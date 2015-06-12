package service

import graph._, GraphM._
import scalaz.syntax.equal._
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import spray.routing._, HttpService._

case class NodeWithRelationships[T](node: Node[T], relationships: Set[Id[_]])

object NodeWithRelationships {
  import spray.json.DefaultJsonProtocol._

  implicit def jsonFormat[T] = jsonFormat2(apply[T])

  def nodeWithRelationships[T : Tag](id: Id[T]) =
    for {
      n <- lookupNode(id)
      e <- lookupEdgesAll(id)
    }
    yield n.map(v => NodeWithRelationships[T](v, e.map(_.to)))

  def addNodeWithRelationships[T : Tag](nodeWithRelationships: NodeWithRelationships[T]) =
    for {
      n <- add(nodeWithRelationships.node)
    }
    yield nodeWithRelationships

  def updateNodeWithRelationships[T : Tag](nodeWithRelationships: NodeWithRelationships[T]) =
    for {
      n <- update(nodeWithRelationships.node)
    }
    yield nodeWithRelationships

}

case class EntityRoute[T : Tag](pathSegment: String) {

  val tableOfContents =
    complete {
      nodes().run(InMemoryGraph)
    }

  // TODO : constrain relationships
  val create = entity(as[NodeWithRelationships[T]]) { nodeWithRelationships =>
    complete {
      NodeWithRelationships.addNodeWithRelationships[T](nodeWithRelationships).run(InMemoryGraph)
    }
  }

  def read(id: Id[T]) =
    complete {
      NodeWithRelationships.nodeWithRelationships(id).run(InMemoryGraph)
    }

    // TODO : constrain relationships
  def update(id: Id[T]) = entity(as[NodeWithRelationships[T]]) { nodeWithRelationships =>
    complete {
      if (nodeWithRelationships.node.id =/= id) {
        sys.error("id mismatch - view and URL id must match")
      }
      NodeWithRelationships.updateNodeWithRelationships[T](nodeWithRelationships).run(InMemoryGraph)
    }
  }

  def delete(id: Id[T]) =
    complete {
      remove(id).run(InMemoryGraph)
      Map("status" -> "deleted")
    }

  val crudRoute =
    pathPrefix(pathSegment)(
      pathEnd(
        get(tableOfContents) ~
        post(create)) ~
      path(new IdMatcher[T])(id =>
        get(read(id)) ~
        put(update(id)) ~
        HttpService.delete(delete(id))
      )
    )

}

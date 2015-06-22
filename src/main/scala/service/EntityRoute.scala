package service

import graph._, /*Graph._,*/ GraphM._
import scalaz.syntax.equal._
import spray.httpx.SprayJsonSupport._
import spray.json._, DefaultJsonProtocol._
import spray.routing._, HttpService._

/*
  {
    node : {}
    relationships : [
      {
        "label" : <optional_label>,
        "node" : <path>,
      },
      ...
    ]
  }
*/

case class NodeWithRelationships[T](node: Node[T], relationships: Set[Relationship])

case class Relationship(nodePath: String)

object Relationship {
  implicit val jsonFormat = jsonFormat1(apply)
}

object NodeWithRelationships {
  import spray.json.DefaultJsonProtocol._

  implicit def jsonFormat[T : JsonFormat] = jsonFormat2(apply[T])

  val NodePathExtractor = "/api/(.*?)/(.*)".r

  def splitNodePath(nodePath: String) = {
    def invalidEntityPath() = sys.error(s"invalid node path $nodePath")
    nodePath match {
      case NodePathExtractor(pathSegment, id) =>
        (NodeRoute.pathSegmentToTag.getOrElse(pathSegment, invalidEntityPath()), id)
      case _ => invalidEntityPath()
    }
  }

  def nodeWithRelationships[T : Tag : JsonFormat](id: Id[T]) =
    for {
      n <- lookupNode(id)
      e <- lookupEdgesAll(id)
    }
    yield n.map(v => NodeWithRelationships[T](v, e.map(e => Relationship(e.to.v))))

  def addNodeWithRelationships[T : Tag : JsonFormat](nodeWithRelationships: NodeWithRelationships[T]) = {
    // TODO : constraints
    for {
      n <- add(nodeWithRelationships.node)
      _ <- GraphM.sequence(nodeWithRelationships.relationships.map { relationship =>
        val (tag, id) = splitNodePath(relationship.nodePath)
        link(n.id.v, Tag[T].v, id, tag)
      })
    }
    yield nodeWithRelationships
  }

  def updateNodeWithRelationships[T : Tag : JsonFormat](nodeWithRelationships: NodeWithRelationships[T]) =
    for {
      n <- update(nodeWithRelationships.node)
    }
    yield nodeWithRelationships

}

case class EntityRoute[T : Tag : JsonFormat](pathSegment: String) {

  val tag = Tag[T]

  val tableOfContents =
    complete {
      nodes().run(InMemoryGraph).map(_.id.v)
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

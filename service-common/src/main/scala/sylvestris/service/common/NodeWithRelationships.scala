package sylvestris.service.common

import sylvestris.core._
import spray.json._, DefaultJsonProtocol._

case class NodeWithRelationships[T](node: Node[T], relationships: Set[Relationship])

object NodeWithRelationships {

  implicit def jsonFormat[T : NodeManifest] = new RootJsonFormat[NodeWithRelationships[T]] {
    def write(n: NodeWithRelationships[T]) = JsObject(
      "id" -> JsString(n.node.id.v),
      "content" -> n.node.content.toJson(NodeManifest[T].jsonFormat),
      "relationships" -> n.relationships.toJson)

    def read(value: JsValue) = value match {
      case v: JsObject => v.getFields("id", "content", "relationships") match {
        case Seq(JsString(id), content, relationships) =>
          NodeWithRelationships(
            Node(Id(id), content.convertTo[T](NodeManifest[T].jsonFormat)),
            relationships.convertTo[Set[Relationship]])
      }
      case _ => deserializationError("id expected")
    }
  }

}

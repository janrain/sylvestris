package sylvestris.core

import spray.json._

object NodeManifest {
  def apply[T : NodeManifest] = implicitly[NodeManifest[T]]
}

case class NodeManifest[T](tag: Tag, jsonFormat: JsonFormat[T])

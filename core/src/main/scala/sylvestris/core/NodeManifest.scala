package sylvestris.core

import spray.json._

object NodeManifest {
  def apply[T : NodeManifest] = implicitly[NodeManifest[T]]
  def apply[T](tag: Tag)(implicit jsonFormat: JsonFormat[T]): NodeManifest[T] =
    new NodeManifest(tag, jsonFormat)
}

class NodeManifest[T](val tag: Tag, val jsonFormat: JsonFormat[T])

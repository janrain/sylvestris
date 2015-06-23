package graph

import spray.json._

object NodeManifest {
  def apply[T : NodeManifest] = implicitly[NodeManifest[T]]
}

trait NodeManifest[T] {
  implicit def tag: Tag[T]
  implicit def jsonFormat: JsonFormat[T]
  implicit def validation: Validation[T]
}

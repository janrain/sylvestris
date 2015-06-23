package graph

object Tag {
  def apply[T : Tag] = implicitly[Tag[T]]

  implicit def fromManifest[T : NodeManifest]: Tag[T] = implicitly[NodeManifest[T]].tag
}

case class Tag[T](v: String)

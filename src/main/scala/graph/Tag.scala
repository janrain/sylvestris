package graph

object Tag {
  def apply[T : Tag] = implicitly[Tag[T]]
}

case class Tag[T](v: String)

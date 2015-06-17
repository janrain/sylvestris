package service

object PathSegment {
  def apply[T : PathSegment] = implicitly[PathSegment[T]]
}

case class PathSegment[T](v: String)

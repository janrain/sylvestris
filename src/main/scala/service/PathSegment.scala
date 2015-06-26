package service

object PathSegment {
  def apply[T : PathSegment]: PathSegment[T] = implicitly[PathSegment[T]]
}

case class PathSegment[T](v: String)

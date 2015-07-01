package sylvestris.service.common

object PathSegment {
  def apply[T : PathSegment]: PathSegment[T] = implicitly[PathSegment[T]]
}

case class PathSegment[T](v: String)

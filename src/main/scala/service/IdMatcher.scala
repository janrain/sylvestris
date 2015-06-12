package service

import graph._
import shapeless.HNil
import spray.http.Uri.Path
import spray.routing._, PathMatcher._

class IdMatcher[T] extends PathMatcher1[Id[T]] {
  def apply(path: Path) = path match {
    case Path.Segment(segment, tail) => Matched(tail, Id[T](segment) :: HNil)
    case _                           => Unmatched
  }
}

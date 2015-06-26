package service

import graph._
import shapeless.HNil
import spray.http.Uri.Path
import spray.routing._, PathMatcher._

object idMatcher extends PathMatcher1[Id] {
  def apply(path: Path) = path match {
    case Path.Segment(segment, tail) => Matched(tail, Id(segment) :: HNil)
    case _                           => Unmatched
  }
}

package graph

import scalaz.Equal

object Edge {
  implicit def eqInstance = Equal.equalA[Edge]
}

case class Edge(label: Option[Label], idA: Id, tagA: Tag, idB: Id, tagB: Tag)

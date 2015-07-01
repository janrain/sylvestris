package sylvestris.core

import scalaz.Equal

object Edge {
  implicit val eqInstance = Equal.equalA[Edge]
}

case class Edge(label: Option[Label], idA: Id, tagA: Tag, idB: Id, tagB: Tag)

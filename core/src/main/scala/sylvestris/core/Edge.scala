package sylvestris.core

import algebra.Eq

object Edge {
  implicit val eqInstance = Eq.fromUniversalEquals[Edge]
}

case class Edge(label: Option[Label], idA: Id, tagA: Tag, idB: Id, tagB: Tag)

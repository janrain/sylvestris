package sylvestris.core

import algebra.Eq

object Tag {
  implicit val eqInstance = Eq.fromUniversalEquals[Tag]
}

case class Tag(v: String)

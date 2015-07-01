package sylvestris.core

import scalaz.Equal

object Tag {
  implicit val eqInstance = Equal.equalA[Tag]
}

case class Tag(v: String)

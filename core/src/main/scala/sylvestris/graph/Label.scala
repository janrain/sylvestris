package sylvestris.core

import scalaz.Equal

object Label {
  implicit val eqInstance = Equal.equalA[Label]
}

case class Label(v: String) extends AnyVal

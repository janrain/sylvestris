package sylvestris.core

import algebra.Eq

object Label {
  implicit val eqInstance = Eq.fromUniversalEquals[Label]
}

case class Label(v: String) extends AnyVal

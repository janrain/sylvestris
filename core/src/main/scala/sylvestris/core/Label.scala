package sylvestris.core

import algebra.Eq

object Label {
  implicit val eqInstance: Eq[Label] = Eq.fromUniversalEquals[Label]
}

case class Label(v: String) extends AnyVal

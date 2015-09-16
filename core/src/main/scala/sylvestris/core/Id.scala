package sylvestris.core

import algebra.Eq

object Id {
  implicit val eqInstance: Eq[Id] = Eq.fromUniversalEquals[Id]
}

case class Id(v: String)

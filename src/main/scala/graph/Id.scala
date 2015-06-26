package graph

import scalaz.Equal

object Id {
  implicit val eqInstance = Equal.equalA[Id]
}

case class Id(v: String)

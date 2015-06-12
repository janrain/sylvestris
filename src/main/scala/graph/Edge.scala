package graph

import scalaz.Equal

case class Edge[T, V](from: Id[T], to: Id[V])

object Edge { implicit def eqInstance[T, U] = Equal.equalA[Edge[T, U]] }

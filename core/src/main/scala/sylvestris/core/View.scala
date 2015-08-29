package sylvestris.core

import cats.data.XorT

trait View[T, U] {
  def get(id: Id): XorT[GraphM, Error, U]
}

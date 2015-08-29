package sylvestris.core

import cats.data.XorT

trait Update[T, U] {
  def update(id: Id, data: U): XorT[GraphM, Error, U]
}

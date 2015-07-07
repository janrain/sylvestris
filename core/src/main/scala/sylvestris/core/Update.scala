package sylvestris.core

import scalaz.EitherT

trait Update[T, U] {
  def update(id: Id, data: U): EitherT[GraphM, Error, U]
}

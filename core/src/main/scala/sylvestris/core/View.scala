package sylvestris.core

import scalaz.EitherT

trait View[T, U] {
  def get(id: Id): EitherT[GraphM, Error, U]
}

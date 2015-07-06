package sylvestris.core

import scalaz.\/

trait View[T, U] {
  def get(id: Id): GraphM[Error \/ U]
}

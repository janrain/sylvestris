package sylvestris.core

import scalaz.\/

trait Update[T, U] {
  def update(id: Id, data: U): GraphM[Error \/ U]
}

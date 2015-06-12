package graph

sealed trait Relationship[T, U]

trait OneToOne[T, U] extends Relationship[T, U]

object OneToOne {
  def apply[T, U] = new OneToOne[T, U] {}
}

trait OneToMany[T, U] extends Relationship[T, U]

object OneToMany {
  def apply[T, U] = new OneToMany[T, U] {}
}

trait ManyToOne[T, U] extends Relationship[T, U]

object ManyToOne {
  def apply[T, U] = new ManyToOne[T, U] {}
}

object Relationship {

  def apply[T, U] = new Relationship[T, U] {}

  implicit def reverseOneToOne[T, U](implicit ev: OneToOne[T, U]) = OneToOne[U, T]

  implicit def reverseOneToMany[T, U](implicit ev: Relationship[T, U]) = ManyToOne[U, T]

  def relationship[T, U](implicit ev: Relationship[T, U]) = true

  def oneToOne[T, U](implicit ev: OneToOne[T, U]) = true

  def oneToMany[T, U](implicit ev: OneToMany[T, U]) = true

  def manyToOne[T, U](implicit ev: ManyToOne[T, U]) = true

}

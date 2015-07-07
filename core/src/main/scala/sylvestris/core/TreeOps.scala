package sylvestris.core

import scalaz.{ \/, EitherT }

case class TreeOps[T : NodeManifest](node: Node[T]) {
  val parentLabel = Label("parent")
  val childLabel = Label("child")

  implicit val toOne = new ToOne[T, T] {
    override val label = Some(Labels(parentLabel, childLabel))
  }

  implicit val toMany = new ToMany[T, T] {
    override val label = Some(Labels(childLabel, parentLabel))
  }

  def parent: EitherT[GraphM, Error, Option[Node[T]]] = node.toOne[T]

  def children: EitherT[GraphM, Error, Set[Node[T]]] = node.toMany[T]

  def parent(p: Option[Node[T]]): EitherT[GraphM, Error, Unit] = parent(p.map(_.id))

  def parent(id: => Option[Id]): EitherT[GraphM, Error, Unit] = node.toOne(id)

  def children(kids: Set[Node[T]]): EitherT[GraphM, Error, Unit] = children(kids.map(_.id))

  def children(ids: => Set[Id]): EitherT[GraphM, Error, Unit] = node.toMany(ids)
}

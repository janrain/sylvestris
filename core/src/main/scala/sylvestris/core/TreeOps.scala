package sylvestris.core

import cats.data.XorT

object TreeOps {
  val parentLabel = Label("parent")
  val childLabel = Label("child")

}

case class TreeOps[T : NodeManifest](node: Node[T]) {
  import TreeOps._

  // https://github.com/puffnfresh/wartremover/issues/149
  @SuppressWarnings(Array("org.brianmckenna.wartremover.warts.NonUnitStatements"))
  implicit val toOne = new OneToOne[T, T] {
    override val label = Some(Labels(parentLabel, childLabel))
  }

  // https://github.com/puffnfresh/wartremover/issues/149
  @SuppressWarnings(Array("org.brianmckenna.wartremover.warts.NonUnitStatements"))
  implicit val toMany = new OneToMany[T, T] {
    override val label = Some(Labels(childLabel, parentLabel))
  }

  def parent: XorT[GraphM, Error, Option[Node[T]]] = node.toOne[T]

  def children: XorT[GraphM, Error, Set[Node[T]]] = node.toMany[T]

  def parent(p: Option[Node[T]]): XorT[GraphM, Error, Unit] = parent(p.map(_.id))

  def parent(id: => Option[Id]): XorT[GraphM, Error, Unit] = node.toOne(id)

  def children(kids: Set[Node[T]]): XorT[GraphM, Error, Unit] = children(kids.map(_.id))

  def children(ids: => Set[Id]): XorT[GraphM, Error, Unit] = node.toMany(ids)
}

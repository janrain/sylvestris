package sylvestris.core

case class TreeOps[T : NodeManifest](node: Node[T]) {
  val parentLabel = Label("parent")
  val childLabel = Label("child")

  implicit val toOne = new ToOne[T, T] {
    override val label = Some(Labels(parentLabel, childLabel))
  }

  implicit val toMany = new ToMany[T, T] {
    override val label = Some(Labels(childLabel, parentLabel))
  }

  def parent: GraphM[Option[Node[T]]] = node.toOne[T]

  def children: GraphM[Set[Node[T]]] = node.toMany[T]

  def parent(p: Option[Node[T]]): GraphM[Unit] = parent(p.map(_.id))

  def parent(id: => Option[Id]) = node.toOne(id)

  def children(kids: Set[Node[T]]): GraphM[Unit] = children(kids.map(_.id))

  def children(ids: => Set[Id]) = node.toMany(ids)
}

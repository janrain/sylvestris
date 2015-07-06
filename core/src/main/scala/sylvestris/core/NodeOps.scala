package sylvestris.core

import GraphM._
import scalaz.{ Id => _, _ }, Scalaz._

abstract class NodeOps[T : NodeManifest] {
  def node: Node[T]

  def toOne[U : NodeManifest : ToOne[T, ?]]: GraphM[Error \/ Option[Node[U]]] =
    EitherT(to[U]).fold(
      _.left,
      nodes =>
        if (nodes.size > 1) Error(s"More than one node returned for $node, $nodes").left
        else nodes.headOption.right
    )

  def toMany[U : NodeManifest : ToMany[T, ?]]: GraphM[Error \/ Set[Node[U]]] = to[U]

  private def to[U : NodeManifest : Relationship[T, ?]]: GraphM[Error \/ Set[Node[U]]] = {
    val rel = Relationship[T, U]
    for {
      edges <- EitherT(getEdges(rel.label.map(_.`t->u`), node.id, rel.tNodeManifest.tag, rel.uNodeManifest.tag))
      // TODO returning only an error here, but there might be multiple errors (one for each getNode)
      nodes <- EitherT(sequence(edges.map { edge => getNode[U](edge.idB) }).map(_.toList.sequenceU))
    } yield nodes.toSet
  }.run

  def toOne[U : NodeManifest : ToOne[T, ?]](uNode: Option[Node[U]]): GraphM[Error \/ Unit] = toOne(uNode.map(_.id))

  def toOne(idU: Option[Id])(implicit relationship: ToOne[_, _]): GraphM[Error \/ Unit] = {
    val tagT = relationship.tNodeManifest.tag
    val tagU = relationship.uNodeManifest.tag
    for {
      _ <- EitherT(removeEdges(node.id, tagT, tagU))
      // TODO there could be multiple errors here, but we're only keeping the first
      _ <- EitherT(sequence(idU.map(id => removeToOneEdges(tagT, id, tagU))).map(_.toList.sequenceU))
      _ <- EitherT(addEdges(idU.map(id => Set(
          Edge(relationship.label.map(_.`t->u`), node.id, tagT, id, tagU),
          Edge(relationship.label.map(_.`u->t`), id, tagU, node.id, tagT))).toSet.flatten))
    } yield {}
  }.run

  def toMany[U : NodeManifest : ToMany[T, ?]](nodes: Set[Node[U]]): GraphM[Error \/ Unit] = toMany(nodes.map(_.id))

  def toMany(ids: Set[Id])(implicit relationship: ToMany[_, _]): GraphM[Error \/ Unit] = {
    val tagT = relationship.tNodeManifest.tag
    val tagU = relationship.uNodeManifest.tag
    for {
      // TODO there could be multiple errors here, but we're only keeping the first
      _ <- EitherT(sequence(ids.map(id => removeToOneEdges(tagT, id, tagU))).map(_.toList.sequenceU))
      _ <- EitherT(addEdges(ids.map(id => Set(
          Edge(relationship.label.map(_.`t->u`), node.id, tagT, id, tagU),
          Edge(relationship.label.map(_.`u->t`), id, tagU, node.id, tagT)).toSet).flatten))
    } yield {}
  }.run

  private def removeToOneEdges(tagT: Tag, idU: Id, tagU: Tag)(implicit relationship: Relationship[_, _]): GraphM[Error \/ Unit] = {
    relationship match {
      case r : ToOne[_, _] =>
        (for {
          edges <- EitherT(getEdges(relationship.label.map(_.`u->t`), idU, tagU, tagT))
          _ <- EitherT(removeEdges(edges.map(e => Edge(e.label, e.idB, e.tagB, e.idA, e.tagA))))
          _ <- EitherT(removeEdges(idU, tagU, tagT))
        } yield {}).run
      case _ => GraphM(().right[Error])
    }
  }

  def tree(implicit ev: Tree[T]): TreeOps[T] = TreeOps(node)
}

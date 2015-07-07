package sylvestris.core

import Graph._
import scalaz.EitherT
import scalaz.Scalaz._

abstract class NodeOps[T : NodeManifest] {
  def node: Node[T]

  def toOne[U : NodeManifest : ToOne[T, ?]]: EitherT[GraphM, Error, Option[Node[U]]] =
    to[U].flatMapF { nodes => GraphM {
      if (nodes.size > 1) Error(s"More than one node returned for $node, $nodes").left
      else nodes.headOption.right
    }}

  def toMany[U : NodeManifest : ToMany[T, ?]]: EitherT[GraphM, Error, Set[Node[U]]] = to[U]

  private def to[U : NodeManifest : Relationship[T, ?]]: EitherT[GraphM, Error, Set[Node[U]]] = {
    val rel = Relationship[T, U]
    for {
      edges <- getEdges(rel.label.map(_.`t->u`), node.id, rel.tNodeManifest.tag, rel.uNodeManifest.tag)
      // TODO returning only an error here, but there might be multiple errors (one for each getNode)
      nodes <- edges.map(edge => getNode[U](edge.idB)).toList.sequenceU
    } yield nodes.toSet
  }

  def toOne[U : NodeManifest : ToOne[T, ?]](uNode: Option[Node[U]]): EitherT[GraphM, Error, Unit] =
    toOne(uNode.map(_.id))

  def toOne(idU: Option[Id])(implicit relationship: ToOne[_, _]): EitherT[GraphM, Error, Unit] = {
    val tagT = relationship.tNodeManifest.tag
    val tagU = relationship.uNodeManifest.tag
    for {
      _ <- removeEdges(node.id, tagT, tagU)
      // TODO : there could be multiple errors here, but we're only keeping the first
      _ <- idU.map(id => removeToOneEdges(tagT, id, tagU)).sequenceU
      _ <- addEdges(idU.map(id => Set(
        Edge(relationship.label.map(_.`t->u`), node.id, tagT, id, tagU),
        Edge(relationship.label.map(_.`u->t`), id, tagU, node.id, tagT))).toSet.flatten)
    } yield {}
  }

  def toMany[U : NodeManifest : ToMany[T, ?]](nodes: Set[Node[U]]): EitherT[GraphM, Error, Unit] =
    toMany(nodes.map(_.id))

  def toMany(ids: Set[Id])(implicit relationship: ToMany[_, _]): EitherT[GraphM, Error, Unit] = {
    val tagT = relationship.tNodeManifest.tag
    val tagU = relationship.uNodeManifest.tag
    for {
      // TODO there could be multiple errors here, but we're only keeping the first
      _ <- ids.map(id => removeToOneEdges(tagT, id, tagU)).toList.sequenceU
      _ <- addEdges(ids.map(id => Set(
          Edge(relationship.label.map(_.`t->u`), node.id, tagT, id, tagU),
          Edge(relationship.label.map(_.`u->t`), id, tagU, node.id, tagT)).toSet).flatten)
    } yield {}
  }

  private def removeToOneEdges(tagT: Tag, idU: Id, tagU: Tag)(implicit relationship: Relationship[_, _])
    : EitherT[GraphM, Error, Unit] = {
    relationship match {
      case r : ToOne[_, _] =>
        for {
          edges <- getEdges(relationship.label.map(_.`u->t`), idU, tagU, tagT)
          _ <- removeEdges(edges.map(e => Edge(e.label, e.idB, e.tagB, e.idA, e.tagA)))
          _ <- removeEdges(idU, tagU, tagT)
        } yield {}
      case _ => EitherT(GraphM(().right[Error]))
    }
  }

  def tree(implicit ev: Tree[T]): TreeOps[T] = TreeOps(node)
}

package sylvestris.core

import Graph._
import cats.data._
import cats.implicits._

abstract class NodeOps[T : NodeManifest] {
  def node: Node[T]

  def toOne[U : NodeManifest : ToOne[T, ?]]: XorT[GraphM, Error, Option[Node[U]]] =
    to[U].flatMapF { nodes => GraphM {
      if (nodes.size > 1) Error(s"More than one node returned for $node, $nodes").left
      else nodes.headOption.right
    }}

  def toMany[U : NodeManifest : ToMany[T, ?]]: XorT[GraphM, Error, Set[Node[U]]] = to[U]

  private def to[U : NodeManifest : Relationship[T, ?]]: XorT[GraphM, Error, Set[Node[U]]] = {
    val rel = Relationship[T, U]
    for {
      edges <- getEdges(rel.label.map(_.`t->u`), node.id, NodeManifest[T].tag, NodeManifest[U].tag)
      // TODO returning only an error here, but there might be multiple errors (one for each getNode)
      nodes <- edges.map(edge => getNode[U](edge.idB)).toList.sequenceU
    } yield nodes.toSet
  }

  def toOne[U : NodeManifest : ToOne[T, ?]](uNode: Option[Node[U]]): XorT[GraphM, Error, Unit] =
    toOne(uNode.map(_.id))

  def toOne(idU: Option[Id])(implicit relationship: ToOne[_, _]): XorT[GraphM, Error, Unit] = {
    val tagT = relationship.tNodeManifest.tag
    val tagU = relationship.uNodeManifest.tag
    for {
      removed <- removeEdges(node.id, tagT, tagU)
      // TODO : there could be multiple errors here, but we're only keeping the first
      _ <- removed.map(e => removeToOneEdges(tagT, e.idB, tagU)).toList.sequenceU
      _ <- addEdges(idU.map(id => Set(
        Edge(relationship.label.map(_.`t->u`), node.id, tagT, id, tagU),
        Edge(relationship.label.map(_.`u->t`), id, tagU, node.id, tagT))).toSet.flatten)
    } yield {}
  }

  def toMany[U : NodeManifest : ToMany[T, ?]](nodes: Set[Node[U]]): XorT[GraphM, Error, Unit] =
    toMany(nodes.map(_.id))

  def toMany(ids: Set[Id])(implicit relationship: ToMany[_, _]): XorT[GraphM, Error, Unit] = {
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
    : XorT[GraphM, Error, Unit] = {
    relationship match {
      case r : ToOne[_, _] =>
        for {
          edges <- getEdges(relationship.label.map(_.`u->t`), idU, tagU, tagT)
          _ <- removeEdges(edges.map(e => Edge(e.label, e.idB, e.tagB, e.idA, e.tagA)))
          _ <- removeEdges(idU, tagU, tagT)
        } yield {}
      case _ => XorT(GraphM(().right[Error]))
    }
  }

  def tree(implicit ev: Tree[T]): TreeOps[T] = TreeOps(node)
}

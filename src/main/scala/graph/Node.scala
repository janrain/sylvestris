package graph

import GraphM._

abstract class NodeOps[T : NodeManifest] {
  def node: Node[T]

  // Error case when more than one node is found
  def toOne[U : NodeManifest : ToOne[T, ?]]: GraphM[Option[Node[U]]] = to[U].map(_.headOption)

  def toMany[U : NodeManifest : ToMany[T, ?]]: GraphM[Set[Node[U]]] = to[U]

  private def to[U : NodeManifest : Relationship[T, ?]] = {
    val rel = Relationship[T, U]
    for {
      edges <- lookupEdges(rel.label.map(_.`t->u`), node.id, rel.tNodeManifest.tag, rel.uNodeManifest.tag)
      nodes <- sequence(edges.map { edge => lookupNode[U](edge.idB) })
    } yield nodes.flatten.toSet
  }

  def toOne[U : NodeManifest : ToOne[T, ?]](uNode: Option[Node[U]]): GraphM[Unit] = toOne(uNode.map(_.id))

  def toOne(idU: Option[Id])(implicit relationship: ToOne[_, _]) = {
    val tagT = relationship.tNodeManifest.tag
    val tagU = relationship.uNodeManifest.tag
    for {
      _ <- removeEdges(node.id, tagT, tagU)
      _ <- sequence(idU.map(id => removeToOneEdges(tagT, id, tagU)))
      _ <- addEdges(idU.map(id => Set(
          Edge(relationship.label.map(_.`t->u`), node.id, tagT, id, tagU),
          Edge(relationship.label.map(_.`u->t`), id, tagU, node.id, tagT))).toSet.flatten)
    } yield {}
  }

  def toMany[U : NodeManifest : ToMany[T, ?]](nodes: Set[Node[U]]): GraphM[Unit] = toMany(nodes.map(_.id))

  def toMany(ids: Set[Id])(implicit relationship: ToMany[_, _]) = {
    val tagT = relationship.tNodeManifest.tag
    val tagU = relationship.uNodeManifest.tag
    for {
      _ <- sequence(ids.map(id => removeToOneEdges(tagT, id, tagU)))
      _ <- addEdges(ids.map(id => Set(
          Edge(relationship.label.map(_.`t->u`), node.id, tagT, id, tagU),
          Edge(relationship.label.map(_.`u->t`), id, tagU, node.id, tagT)).toSet).flatten)
    } yield {}
  }

  private def removeToOneEdges(tagT: Tag, idU: Id, tagU: Tag)(implicit relationship: Relationship[_, _]) = {
    relationship match {
      case r : ToOne[_, _] =>
        for {
          edges <- lookupEdges(relationship.label.map(_.`u->t`), idU, tagU, tagT)
          _ <- removeEdges(edges.map(e => Edge(e.label, e.idB, e.tagB, e.idA, e.tagA)))
          _ <- removeEdges(idU, tagU, tagT)
        } yield {}
      case _ => GraphM(())
    }
  }

  def tree(implicit ev: Tree[T]): TreeOps[T] = TreeOps(node)
}

object Node {
  implicit def toNodeOps[T : NodeManifest](n: Node[T]): NodeOps[T] = new NodeOps[T] {
    val node: Node[T] = n
  }
}

case class Node[T](id: Id, content: T)

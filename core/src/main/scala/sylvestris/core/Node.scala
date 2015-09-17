package sylvestris.core

import algebra.Eq

object Node {
  // https://github.com/puffnfresh/wartremover/issues/149
  @SuppressWarnings(Array("org.brianmckenna.wartremover.warts.NonUnitStatements"))
  implicit def toNodeOps[T : NodeManifest](n: Node[T]): NodeOps[T] = new NodeOps[T] {
    val node: Node[T] = n
  }

  implicit def eqInstance[T]: Eq[Node[T]] = Eq.fromUniversalEquals[Node[T]]
}

case class Node[T](id: Id, content: T)

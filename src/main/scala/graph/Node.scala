package graph

object Node {
  implicit def toNodeOps[T : NodeManifest](n: Node[T]): NodeOps[T] = new NodeOps[T] {
    val node: Node[T] = n
  }
}

case class Node[T](id: Id, content: T)

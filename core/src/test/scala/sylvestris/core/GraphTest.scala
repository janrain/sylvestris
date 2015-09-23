package sylvestris.core

import cats.implicits._
import org.scalacheck._, Prop._, Shapeless._
import sylvestris.core.Graph._
import sylvestris.core.fixtures._, model._

@SuppressWarnings(Array(
  "org.brianmckenna.wartremover.warts.Any",
  "org.brianmckenna.wartremover.warts.AsInstanceOf",
  "org.brianmckenna.wartremover.warts.NonUnitStatements",
  "org.brianmckenna.wartremover.warts.Throw"))
abstract class GraphTest extends Properties("Graph") {
  // TODO : this signature will change once transact is in place
  def withGraph[T](f: Graph => T): T

  property("addNode") = forAll { (node: Node[Content1]) => withGraph { g =>
    runAssertIsRight(g) {
      addNode(node)
    }

    nodes[Content1].value.run(g) === Set(node).right[List[Error]]
  }}

  property("getNode") = forAll { (node: Node[Content1]) => withGraph { g =>
    runAssertIsRight(g) {
      addNode(node)
    }
    getNode[Content1](node.id).value.run(g) === node.right
  }}

  property("updateNode") = forAll { (node: Node[Content1], newContent: Content1) => withGraph { g =>
    runAssertIsRight(g) {
      addNode(node)
    }
    val newNode = Node(id = node.id, content = newContent)
    updateNode(newNode).value.run(g)
    nodes[Content1].value.run(g) === Set(newNode).right[List[Error]]
  }}

  property("removeNode") = forAll { (node: Node[Content1]) => withGraph { g =>
    runAssertIsRight(g) {
      addNode(node)
    }
    removeNode[Content1](node.id).value.run(g)
    nodes[Content1].value.run(g) === Set.empty[Node[Content1]].right[List[Error]]
  }}

  property("addEdges") = forAll { (node1: Node[Content1], node2: Node[Content2]) =>
    (node1.id =!= node2.id) ==> withGraph { g =>
    val edge = Edge(None, node1.id, Content1.nodeManifest.tag, node2.id, Content2.nodeManifest.tag)
    runAssertIsRight(g) {
      for {
        _ <- addNode(node1)
        _ <- addNode(node2)
        _ <- addEdges(Set(edge))
      } yield {}
    }

    getEdges(node1.id, Content1.nodeManifest.tag).value.run(g) === Set(edge).right[Error]
  }}

  property("removeEdges(Set[Edge])") = forAll { (node1: Node[Content1], node2: Node[Content2]) =>
    (node1.id =!= node2.id) ==> withGraph { g =>
    val edge = Edge(None, node1.id, Content1.nodeManifest.tag, node2.id, Content2.nodeManifest.tag)
    runAssertIsRight(g) {
      for {
        _ <- addNode(node1)
        _ <- addNode(node2)
        _ <- addEdges(Set(edge))
      } yield {}
    }
    val removed = removeEdges(Set(edge)).value.run(g)
    removed === Set(edge).right[Error] &&
      getEdges(node1.id, Content1.nodeManifest.tag).value.run(g) === Set.empty[Edge].right[Error]
  }}

  property("removeEdges") = forAll { (node1: Node[Content1], node2: Node[Content2]) =>
    (node1.id =!= node2.id) ==> withGraph { g =>
    val edge = Edge(None, node1.id, Content1.nodeManifest.tag, node2.id, Content2.nodeManifest.tag)
    runAssertIsRight(g) {
      for {
        _ <- addNode(node1)
        _ <- addNode(node2)
        _ <- addEdges(Set(edge))
      } yield {}
    }
    val removed = removeEdges(node1.id, Content1.nodeManifest.tag, Content2.nodeManifest.tag).value.run(g)
    removed === Set(edge).right &&
      getEdges(node1.id, Content1.nodeManifest.tag).value.run(g) === Set.empty[Edge].right[Error]
  }}

  property("getEdges filters on type") = forAll {
    (node1: Node[Content1], node2: Node[Content1], node3: Node[Content2]) =>
    (node1.id =!= node2.id && node2.id =!= node3.id) ==> withGraph { g =>
    val edge1 = Edge(None, node1.id, Content1.nodeManifest.tag, node2.id, Content1.nodeManifest.tag)
    val edge2 = Edge(None, node1.id, Content1.nodeManifest.tag, node3.id, Content2.nodeManifest.tag)
    runAssertIsRight(g) {
      for {
        _ <- addNode(node1)
        _ <- addNode(node2)
        _ <- addNode(node3)
        _ <- addEdges(Set(edge1, edge2))
      } yield {}
    }
    val foundEdges = getEdges(None, node1.id, Content1.nodeManifest.tag, Content2.nodeManifest.tag)
    foundEdges.value.run(g) === Set(edge2).right[Error]
  }}
}

object InMemoryGraphTest extends GraphTest {
  def withGraph[T](f: Graph => T): T = f(InMemoryGraph())
}

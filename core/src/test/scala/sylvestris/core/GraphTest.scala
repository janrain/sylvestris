package sylvestris.core

import org.scalacheck._, Arbitrary._, Prop._, Shapeless._
import scalaz._, Scalaz._
import shapeless.contrib.scalaz._
import spray.json._, DefaultJsonProtocol._
import sylvestris.core.Graph._

abstract class GraphTest extends Properties("Graph") {
  def newGraph: Graph

  object Content1 {
    implicit object nodeManifest extends NodeManifest[Content1] {
      implicit val tag = Tag("content")
      implicit val jsonFormat = jsonFormat1(apply)
    }
  }
  case class Content1(v: String)

  object Content2 {
    implicit object nodeManifest extends NodeManifest[Content2] {
      implicit val tag = Tag("content2")
      implicit val jsonFormat = jsonFormat1(apply)
    }
  }
  case class Content2(v: String)

  property("addNode") = forAll { (node: Node[Content1]) =>
    val graph = newGraph
    addNode(node).run.run(graph)
    nodes[Content1].run.run(graph) === Set(node).right[List[Error]]
  }

  property("getNode") = forAll { (node: Node[Content1]) =>
    val graph = newGraph
    addNode(node).run.run(graph)
    getNode[Content1](node.id).run.run(graph) === node.right
  }

  property("updateNode") = forAll { (node: Node[Content1], newContent: Content1) =>
    val graph = newGraph
    addNode(node).run.run(graph)
    val newNode = Node(id = node.id, content = newContent)
    updateNode(newNode).run.run(graph)
    nodes[Content1].run.run(graph) === Set(newNode).right[List[Error]]
  }

  property("removeNode") = forAll { (node: Node[Content1]) =>
    val graph = newGraph
    addNode(node).run.run(graph)
    removeNode[Content1](node.id).run.run(graph)
    nodes[Content1].run.run(graph) === Set.empty[Node[Content1]].right[List[Error]]
  }

  property("addEdges") = forAll { (node1: Node[Content1], node2: Node[Content2]) =>
    val graph = newGraph
    val edge = Edge(None, node1.id, Content1.nodeManifest.tag, node2.id, Content2.nodeManifest.tag)
    (for {
      _ <- addNode(node1)
      _ <- addNode(node1)
      _ <- addEdges(Set(edge))
    } yield {}).run.run(graph)
    getEdges(node1.id, Content1.nodeManifest.tag).run.run(graph) === Set(edge).right[Error]
  }

  property("removeEdges(Set[Edge])") = forAll { (node1: Node[Content1], node2: Node[Content2]) =>
    val graph = newGraph
    val edge = Edge(None, node1.id, Content1.nodeManifest.tag, node2.id, Content2.nodeManifest.tag)
    (for {
      _ <- addNode(node1)
      _ <- addNode(node1)
      _ <- addEdges(Set(edge))
    } yield {}).run.run(graph)
    removeEdges(Set(edge)).run.run(graph)
    getEdges(node1.id, Content1.nodeManifest.tag).run.run(graph) === Set.empty[Edge].right[Error]
  }

  property("removeEdges") = forAll { (node1: Node[Content1], node2: Node[Content2]) =>
    val graph = newGraph
    val edge = Edge(None, node1.id, Content1.nodeManifest.tag, node2.id, Content2.nodeManifest.tag)
    (for {
      _ <- addNode(node1)
      _ <- addNode(node1)
      _ <- addEdges(Set(edge))
    } yield {}).run.run(graph)
    removeEdges(node1.id, Content1.nodeManifest.tag, Content2.nodeManifest.tag).run.run(graph)
    getEdges(node1.id, Content1.nodeManifest.tag).run.run(graph) === Set.empty[Edge].right[Error]
  }

  property("getEdges filters on type") = forAll {
    (node1: Node[Content1], node2: Node[Content1], node3: Node[Content2]) =>
    val graph = newGraph
    val edge1 = Edge(None, node1.id, Content1.nodeManifest.tag, node2.id, Content1.nodeManifest.tag)
    val edge2 = Edge(None, node1.id, Content1.nodeManifest.tag, node3.id, Content2.nodeManifest.tag)
    (for {
      _ <- addNode(node1)
      _ <- addNode(node1)
      _ <- addEdges(Set(edge1, edge2))
    } yield {}).run.run(graph)

    val foundEdges = getEdges(None, node1.id, Content1.nodeManifest.tag, Content2.nodeManifest.tag)
    foundEdges.run.run(graph) === Set(edge2).right[Error]
  }
}

object InMemoryGraphTest extends GraphTest {
  def newGraph = InMemoryGraph()
}

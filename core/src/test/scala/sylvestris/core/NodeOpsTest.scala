package sylvestris.core

import cats.implicits._
import org.scalacheck._, Prop._, Shapeless._
import spray.json._, DefaultJsonProtocol._
import sylvestris.core._, Graph._, Relationship._
import sylvestris.core.fixtures._, model._

//TODO make this a trait and use the withgraph business...
@SuppressWarnings(Array(
  "org.brianmckenna.wartremover.warts.Any",
  "org.brianmckenna.wartremover.warts.AsInstanceOf",
  "org.brianmckenna.wartremover.warts.NonUnitStatements",
  "org.brianmckenna.wartremover.warts.Throw"))
abstract class NodeOpsTest extends Properties("NodeOpsTest") {
  def withGraph[T](f: Graph => T): T

  property("toOne get") = forAll { (node1: Node[Content1], node2: Node[Content2]) =>
    (node1.id =!= node2.id) ==> withGraph { g =>
      implicit val oneToOne = new OneToOne[Content1, Content2]
      val toEdge = Edge(None, node1.id, Content1.nodeManifest.tag, node2.id, Content2.nodeManifest.tag)
      val fromEdge = Edge(None, node2.id, Content2.nodeManifest.tag, node1.id, Content1.nodeManifest.tag)
      runAssertIsRight(g) {
        for {
          _ <- addNode(node1)
          _ <- addNode(node2)
          _ <- addEdges(Set(toEdge))
          _ <- addEdges(Set(fromEdge))
        } yield {}
      }
      node1.toOne[Content2].value.run(g) === Some(node2).right &&
      node2.toOne[Content1].value.run(g) === Some(node1).right
    }
  }

  property("toMany get") = forAll { (node1: Node[Content1], node2: Node[Content2], node3: Node[Content2]) =>
    (node1.id =!= node2.id && node2.id =!= node3.id) ==> withGraph { g =>
      implicit val oneToMany = new OneToMany[Content1, Content2]
      val edges = Set(node2, node3).map(n => Set(
          Edge(None, node1.id, Content1.nodeManifest.tag, n.id, Content2.nodeManifest.tag),
          Edge(None, n.id, Content2.nodeManifest.tag, node1.id, Content1.nodeManifest.tag))).flatten
      runAssertIsRight(g) {
        for {
          _ <- addNode(node1)
          _ <- addNode(node2)
          _ <- addNode(node3)
          _ <- addEdges(edges)
        } yield {}
      }

      node1.toMany[Content2].value.run(g) === Set(node2, node3).right &&
      node2.toOne[Content1].value.run(g) === Some(node1).right &&
      node3.toOne[Content1].value.run(g) === Some(node1).right
    }
  }

  property("toOne(Node) set") = forAll { (node1: Node[Content1], node2: Node[Content2]) =>
    (node1.id =!= node2.id) ==> withGraph { g =>
      implicit val oneToOne = new OneToOne[Content1, Content2]
      runAssertIsRight(g) {
        for {
          _ <- addNode(node1)
          _ <- addNode(node2)
          _ <- node1.toOne[Content2](Some(node2))
        } yield {}
      }

      val expectedTo = Edge(None, node1.id, Content1.nodeManifest.tag, node2.id, Content2.nodeManifest.tag)
      val expectedFrom = Edge(None, node2.id, Content2.nodeManifest.tag, node1.id, Content1.nodeManifest.tag)

      getEdges(node1.id, Content1.nodeManifest.tag).value.run(g) === Set(expectedTo).right &&
      getEdges(node2.id, Content2.nodeManifest.tag).value.run(g) === Set(expectedFrom).right
    }
  }

  property("toOne(Node) clear") = forAll { (node1: Node[Content1], node2: Node[Content2]) =>
    (node1.id =!= node2.id) ==> withGraph { g =>
      implicit val oneToOne = new OneToOne[Content1, Content2]
      val edges = Set(
        Edge(None, node1.id, Content1.nodeManifest.tag, node2.id, Content2.nodeManifest.tag),
        Edge(None, node2.id, Content2.nodeManifest.tag, node1.id, Content1.nodeManifest.tag))
        runAssertIsRight(g) {
          for {
            _ <- addNode(node1)
            _ <- addNode(node2)
            _ <- addEdges(edges)
          } yield {}
        }

      // TODO having to type the None is annoying
      node1.toOne[Content2](Option.empty[Node[Content2]]).value.run(g)

      getEdges(node1.id, Content1.nodeManifest.tag).value.run(g) === Set.empty[Edge].right &&
      getEdges(node2.id, Content2.nodeManifest.tag).value.run(g) === Set.empty[Edge].right
    }
  }


  // TODO toOne replace
}

object InMemoryNodeOpsTest extends NodeOpsTest {
  def withGraph[T](f: Graph => T): T = f(InMemoryGraph())
}

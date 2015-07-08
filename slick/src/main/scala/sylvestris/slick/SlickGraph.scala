package sylvestris.slick

import scalaz.{ \/, EitherT }
import scalaz.std.list._
import scalaz.syntax._, either._, traverse._
import scala.slick.ast.ColumnOption.DBType
import scala.slick.driver.PostgresDriver.simple.{ Tag => _, _ }
import scala.slick.jdbc.meta.MTable
import spray.json._
import sylvestris.core._
import sylvestris.slick.SlickGraph.{ Node => SlickNode, nodes => slickNodes, Edge => SlickEdge, edges => slickEdges }

// TODO : update to slick 3.0
// TODO : .transactional

class SlickGraph(implicit session: Session) extends Graph {

  for (t <- List(slickNodes, slickEdges) if MTable.getTables(t.baseTableRow.tableName).list.isEmpty) {
    t.ddl.create
  }

  def slickNodeToNode[T : NodeManifest](v: SlickNode): Error \/ Node[T] =
    \/.fromTryCatchNonFatal(v.content.parseJson.convertTo[T](NodeManifest[T].jsonFormat))
      .bimap(t => Error(s"unable to parse $v to Node", Some(t)), Node[T](Id(v.id), _))

  def slickEdgeToEdge(v: SlickEdge): Edge = Edge(v.label.map(Label(_)), Id(v.aId), Tag(v.aTag), Id(v.bId), Tag(v.aTag))

  def nodes[T : NodeManifest](): EitherT[GraphM, List[Error], Set[Node[T]]] = EitherTGraphM {
    slickNodes.list.map(slickNodeToNode[T]).sequenceU.bimap(List(_), _.toSet)
  }

  def getNode[T : NodeManifest](id: Id): EitherT[GraphM, Error, Node[T]] = EitherTGraphM {
    slickNodes
      .filter(d => d.id === id.v && d.tag === NodeManifest[T].tag.v)
      .list
      .map(slickNodeToNode[T])
      .sequenceU
      .flatMap {
        case h :: Nil => h.right
        case Nil => Error(s"$id not found").left
        case nodes => Error(s"more than one node found for $id, $nodes").left
      }
  }

  def addNode[T : NodeManifest](node: Node[T]): EitherT[GraphM, Error, Node[T]] = EitherTGraphM {
    implicit val jsonFormat = NodeManifest[T].jsonFormat

    if (slickNodes.filter(_.id === node.id.v).run.nonEmpty) {
      Error(s"$node already defined").left
    }
    else {
      slickNodes += SlickNode(node.id.v, NodeManifest[T].tag.v, node.content.toJson.compactPrint)
      node.right
    }
  }

  def updateNode[T : NodeManifest](node: Node[T]): EitherT[GraphM, Error, Node[T]] = ???

  def removeNode[T : NodeManifest](id: Id): EitherT[GraphM, Error, Node[T]] =
    getNode(id).flatMap { node => EitherTGraphM {
      val deletedCount = slickNodes.filter(_.id === id.v).delete
      if (deletedCount < 1) Error(s"$id not found").left
      else node.right
    }}

  // TODO : current DocumentGraph not storing tags
  def getEdges(id: Id, tag: Tag): EitherT[GraphM, Error, Set[Edge]] = EitherTGraphM {
    slickEdges
      .filter(e => e.aId === id.v && e.aTag === tag.v)
      .list
      .map(slickEdgeToEdge)
      .toSet
      .right
      // .sequenceU//.bimap(List(_), _.toSet)
  }

  def getEdges(label: Option[Label], idA: Id, tagA: Tag, tagB: Tag): EitherT[GraphM, Error, Set[Edge]] = ???
  def addEdges(edges: Set[Edge]): EitherT[GraphM, Error, Set[Edge]] = ???
  def removeEdges(edges: Set[Edge]): EitherT[GraphM, Error, Set[Edge]] = ???
  def removeEdges(idA: Id, tagA: Tag, tagB: Tag): EitherT[GraphM, Error, Set[Edge]] = ???

}

object SlickGraph {
  import scala.slick.driver.PostgresDriver.simple.Tag

  case class Node(id: String, tag: String, content: String)

  // TODO : migration
  // - rename table documents → nodes
  // - rename nodes : id → id
  // - rename nodes : type → tag
  // - rename edges : name → label
  // - rename edges : from → a_id
  // - add column a_tag to edges
  // - rename edges : to → b_id
  // - add column b_tag to edges

  class Nodes(t: Tag) extends Table[Node](t, "nodes") {
    // TODO : id -> id?
    def id = column[String]("id", O.PrimaryKey)
    def tag = column[String]("tag")
    def content = column[String]("content", DBType("TEXT"))
    def * = (id, tag, content) <> (Node.tupled, Node.unapply)
    def idxType = index("idx_type", tag)
  }
  val nodes = TableQuery[Nodes]

  // TODO : update variable name to be in line with Edge

  case class Edge(label: Option[String], aId: String, aTag: String, bId: String, bTag: String)

  class Edges(t: Tag) extends Table[Edge](t, "edges") {
    def label = column[Option[String]]("label")
    def aId = column[String]("a_id")
    def aTag = column[String]("a_tag")
    def bId = column[String]("b_id")
    def bTag = column[String]("b_tag")
    def * = (label, aId, aTag, bId, bTag) <> (Edge.tupled, Edge.unapply)
    // TODO : do we want delete cascade?
    def aFk = foreignKey("a_fk", aId, nodes)(_.id, onDelete = ForeignKeyAction.Cascade)
    def bFk = foreignKey("to_fk", bId, nodes)(_.id, onDelete = ForeignKeyAction.Cascade)
    def idx = index("idx_all", (label, aId, aTag, bId, bTag), unique = true)
    def idxA = index("idx_a", (aId, aTag))
    def idxB = index("idx_b", (bId, bTag))
  }
  val edges = TableQuery[Edges]

}

package sylvestris.slick

import cats.data._
import cats.implicits._
import scala.slick.ast.ColumnOption.DBType
import scala.slick.driver.PostgresDriver.simple.{ Tag => _, _ }
import scala.slick.jdbc.meta.MTable
import spray.json._
import sylvestris.core, core._
import sylvestris.slick.SlickGraph._

@SuppressWarnings(Array("org.brianmckenna.wartremover.warts.NonUnitStatements"))
class SlickGraph(implicit session: Session) extends Graph {

  for (t <- List(slickNodes, slickEdges) if MTable.getTables(t.baseTableRow.tableName).list.isEmpty) {
    t.ddl.create
  }

  def nodes[T : NodeManifest](): XorT[GraphM, List[Error], Set[Node[T]]] = XorTGraphM {
    slickNodes.list.map(slickNodeToNode[T]).sequenceU.bimap(List(_), _.toSet)
  }

  def getNode[T : NodeManifest](id: Id): XorT[GraphM, Error, Node[T]] = slick {
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

  def addNode[T : NodeManifest](node: Node[T]): XorT[GraphM, Error, Node[T]] = slick {
    if (slickNodes.filter(_.id === node.id.v).run.nonEmpty) {
      Error(s"$node already defined").left
    }
    else {
      slickNodes += nodeToSlickNode(node)
      node.right
    }
  }

  def updateNode[T : NodeManifest](node: Node[T]): XorT[GraphM, Error, Node[T]] = slick {
    val updatedCount = slickNodes.filter(_.id === node.id.v).update(nodeToSlickNode(node))
    if (updatedCount =!= 1) Error(s"updated $updatedCount for $node").left
    else node.right
  }

  def removeNode[T : NodeManifest](id: Id): XorT[GraphM, Error, Node[T]] =
    getNode(id).flatMap { node => slick {
      val deletedCount = slickNodes.filter(_.id === id.v).delete
      if (deletedCount < 1) Error(s"$id not deleted").left
      else node.right
    }}

  def getEdges(id: Id, tag: Tag): XorT[GraphM, Error, Set[Edge]] = slick {
    filterEdgesQuery(id, tag)
      .list
      .map(slickEdgeToEdge)
      .toSet
      .right
  }

  def getEdges(label: Option[Label], idA: Id, tagA: Tag, tagB: Tag): XorT[GraphM, Error, Set[Edge]] = slick {
    filterEdgesQuery(label, idA, tagA, tagB)
      .list
      .map(slickEdgeToEdge)
      .toSet
      .right
  }

  def addEdges(edges: Set[Edge]): XorT[GraphM, Error, Set[Edge]] = slick {
    slickEdges ++= edges.map(edgeToSlickEdge)
    edges.right
  }

  def removeEdges(edges: Set[Edge]): XorT[GraphM, Error, Set[Edge]] = slick {
    val deletedCount = edges.map(filterEdgesQuery).reduce(_++_).delete
    if (deletedCount =!= edges.size) Error(s"$deletedCount of ${edges.size} deleted, $edges").left
    else edges.right
  }

  def removeEdges(idA: Id, tagA: Tag, tagB: Tag): XorT[GraphM, Error, Set[Edge]] =
    getEdges(None, idA, tagA, tagB).flatMap { edges => slick {
      val deletedCount = filterEdgesQuery(idA, tagA, tagB).delete
      if (deletedCount =!= edges.size) Error(s"$deletedCount of ${edges.size} deleted, $edges").left
      else edges.right
    }}

}

object SlickGraph {
  import scala.slick.driver.PostgresDriver.simple.Tag

  case class SlickNode(id: String, tag: String, content: String)

  // TODO : migration
  // - rename table documents → nodes
  // - rename nodes : poid → id
  // - rename nodes : type → tag
  // - rename edges : name → label
  // - rename edges : from → a_id
  // - add column a_tag to edges
  // - rename edges : to → b_id
  // - add column b_tag to edges

  class SlickNodes(t: Tag) extends Table[SlickNode](t, "nodes") {
    def id = column[String]("id", O.PrimaryKey)
    def tag = column[String]("tag")
    def content = column[String]("content", DBType("TEXT"))
    def * = (id, tag, content) <> (SlickNode.tupled, SlickNode.unapply)
    def idxType = index("idx_type", tag)
  }
  val slickNodes = TableQuery[SlickNodes]

  // TODO : update variable names to be in line with Edge

  case class SlickEdge(label: Option[String], idA: String, tagA: String, idB: String, tagB: String)

  class SlickEdges(t: Tag) extends Table[SlickEdge](t, "edges") {
    def label = column[Option[String]]("label")
    def idA = column[String]("a_id")
    def tagA = column[String]("a_tag")
    def idB = column[String]("b_id")
    def tagB = column[String]("b_tag")
    def * = (label, idA, tagA, idB, tagB) <> (SlickEdge.tupled, SlickEdge.unapply)
    // TODO : do we want delete cascade?
    def aFk = foreignKey("a_fk", idA, slickNodes)(_.id, onDelete = ForeignKeyAction.Cascade)
    def bFk = foreignKey("to_fk", idB, slickNodes)(_.id, onDelete = ForeignKeyAction.Cascade)
    def idx = index("idx_all", (label, idA, tagA, idB, tagB), unique = true)
    def idxA = index("idx_a", (idA, tagA))
    def idxB = index("idx_b", (idB, tagB))
  }
  val slickEdges = TableQuery[SlickEdges]

  def filterEdgesQuery(idA: Id, tagA: core.Tag)
    : Query[SlickEdges, SlickEdges#TableElementType, Seq] =
    filterEdgesQuery(None, idA, tagA, None, None)

  def filterEdgesQuery(idA: Id, tagA: core.Tag, tagB: core.Tag): Query[SlickEdges, SlickEdges#TableElementType, Seq] =
    filterEdgesQuery(None, idA, tagA, None, Some(tagB))

  def filterEdgesQuery(label: Option[Label], idA: Id, tagA: core.Tag, tagB: core.Tag)
    : Query[SlickEdges, SlickEdges#TableElementType, Seq] =
    filterEdgesQuery(label, idA, tagA, None, Some(tagB))

  def filterEdgesQuery(edge: Edge): Query[SlickEdges, SlickEdges#TableElementType, Seq] =
    filterEdgesQuery(edge.label, edge.idA, edge.tagA, Some(edge.idB), Some(edge.tagB))

  def filterEdgesQuery(label: Option[Label], idA: Id, tagA: core.Tag, idB: Option[Id], tagB: Option[core.Tag])
    : Query[SlickEdges, SlickEdges#TableElementType, Seq] = {
      val q1 = slickEdges.filter(e => e.idA === idA.v && e.tagA === tagA.v)
      val q2 = label.fold(q1)(l => q1.filter(_.label === label.map(_.v)))
      val q3 = idB.fold(q2)(i => q2.filter(_.idB === i.v))
      tagB.fold(q3)(t => q3.filter(_.tagB === t.v))
    }

  def slickNodeToNode[T : NodeManifest](v: SlickNode): Error Xor Node[T] =
    Xor.fromTryCatch(v.content.parseJson.convertTo[T](NodeManifest[T].jsonFormat))
      .bimap(t => Error(s"unable to parse $v to Node", Some(t)), Node[T](Id(v.id), _))

  def nodeToSlickNode[T : NodeManifest](v: Node[T]): SlickNode =
    SlickNode(v.id.v, NodeManifest[T].tag.v, v.content.toJson(NodeManifest[T].jsonFormat).compactPrint)

  def slickEdgeToEdge(v: SlickEdge): Edge = Edge(v.label.map(Label(_)), Id(v.idA), Tag(v.tagA), Id(v.idB), Tag(v.tagB))

  def edgeToSlickEdge(v: Edge): SlickEdge = SlickEdge(v.label.map(_.v), v.idA.v, v.tagA.v, v.idB.v, v.tagB.v)

  def slick[T](op: => Error Xor T): XorT[GraphM, Error, T] = XorTGraphM {
    Xor.fromTryCatch(op).fold(e => Error("unhandled slick error", Some(e)).left, identity)
  }

}

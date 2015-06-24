package graph

import org.reflections.Reflections
import scalaz._, Scalaz._

sealed class Relationship[T : Tag, U : Tag](
  val labelUT: Option[Label] = None,
  val labelTU: Option[Label] = None) {

  val tTag = Tag[T]
  val uTag = Tag[U]
}

class OneToOne[T : Tag, U : Tag] extends Relationship[T, U]

class OneToMany[T : Tag, U : Tag] extends Relationship[T, U]

class ManyToOne[T : Tag, U : Tag] extends Relationship[T, U]

class Parent[T : Tag] extends Relationship[T, T](Some(Label("parent")), Some(Label("child")))

case class RelationshipMappings(packagePrefix: String) {
  // TODO this needs to be done for all relationship types
  val mapping = new Reflections(packagePrefix).getSubTypesOf(classOf[OneToOne[_, _]]).toArray.toList
    .map { i =>
      val z = i.asInstanceOf[Class[OneToOne[_, _]]].newInstance
      Map(
        z.tTag.v -> List[Relationship[_, _]](z),
        z.uTag.v -> List[Relationship[_, _]](new OneToOne()(z.uTag, z.tTag)))
    }
    .suml
}

object Relationship {

  def apply[T : Tag, U : Tag] = new Relationship[T, U]

  implicit def reverseOneToOne[T : Tag, U : Tag](implicit ev: OneToOne[T, U]) = new OneToOne[U, T]

  implicit def reverseOneToMany[T : Tag, U : Tag](implicit ev: OneToMany[T, U]) = new ManyToOne[U, T]

  def relationship[T, U](implicit ev: Relationship[T, U]) = true

  def oneToOne[T, U](implicit ev: OneToOne[T, U]) = true

  def oneToMany[T, U](implicit ev: OneToMany[T, U]) = true

  def manyToOne[T, U](implicit ev: ManyToOne[T, U]) = true

  // def validRelationship

  // def relationships(packagePrefix: String): List[Relationship[_, _]] = {
  //   val reflections = new Reflections(packagePrefix)
  //
  //   // TODO : brittle and verbose for now; looks like we can't get subtypes of subtypes; revisit
  //   val relationships = List(
  //     reflections.getSubTypesOf(classOf[graph.OneToOne[_, _]]),
  //     reflections.getSubTypesOf(classOf[graph.OneToMany[_, _]]),
  //     reflections.getSubTypesOf(classOf[graph.ManyToOne[_, _]]))
  //
  //   relationships
  //     .flatMap(_.toArray.toList)
  //     .map(_.asInstanceOf[Class[graph.Relationship[_, _]]].newInstance)
  // }

}

package graph

import org.reflections.Reflections
import scalaz._, Scalaz._

class Relationship[T : Tag, U : Tag] {
  val tTag = Tag[T]
  val uTag = Tag[U]
}

class OneToOne[T : Tag, U : Tag] extends Relationship[T, U]

object OneToOne {
  def apply[T : Tag, U : Tag] = new OneToOne[T, U] {}
}

class OneToMany[T : Tag, U : Tag] extends Relationship[T, U]

object OneToMany {
  def apply[T : Tag, U : Tag] = new OneToMany[T, U] {}
}

class ManyToOne[T : Tag, U : Tag] extends Relationship[T, U]

object ManyToOne {
  def apply[T : Tag, U : Tag] = new ManyToOne[T, U] {}
}

case class RelationshipMappings(packagePrefix: String) {
  val mapping = new Reflections(packagePrefix).getSubTypesOf(classOf[OneToOne[_, _]]).toArray.toList
    .map { i =>
      val z = i.asInstanceOf[Class[OneToOne[_, _]]].newInstance
      val t = z.tTag.v
      val u = z.uTag.v
      Map(t -> List(u), u -> List(t))
    }
    .suml
}

object Relationship {

  def apply[T : Tag, U : Tag] = new Relationship[T, U]

  implicit def reverseOneToOne[T : Tag, U : Tag](implicit ev: OneToOne[T, U]) = OneToOne[U, T]

  implicit def reverseOneToMany[T : Tag, U : Tag](implicit ev: Relationship[T, U]) = ManyToOne[U, T]

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

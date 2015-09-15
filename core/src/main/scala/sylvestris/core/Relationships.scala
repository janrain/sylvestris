package sylvestris.core

import cats.implicits._
import catsclaw.implicits._
import org.reflections.Reflections

sealed trait Relationship[T, U] {
  def tNodeManifest: NodeManifest[T]
  def uNodeManifest: NodeManifest[U]

  type ReverseRelationship <: Relationship[U, T]

  def reverse: ReverseRelationship

  case class Labels(`t->u`: Label, `u->t`: Label)

  def label: Option[Labels] = None
}

abstract class ToOne[T : NodeManifest, U : NodeManifest] extends Relationship[T, U] {
  def tNodeManifest = NodeManifest[T]
  def uNodeManifest = NodeManifest[U]
}

abstract class ToMany[T : NodeManifest, U : NodeManifest] extends Relationship[T, U] {
  def tNodeManifest = NodeManifest[T]
  def uNodeManifest = NodeManifest[U]
}

object OneToOne {
  def apply[T : NodeManifest, U : NodeManifest] = new OneToOne[T, U]
}

class OneToOne[T : NodeManifest, U : NodeManifest] extends ToOne[T, U] {
  type ReverseRelationship = OneToOne[U, T]

  def reverse = OneToOne[U, T]
}

object OneToMany {
  def apply[T : NodeManifest, U : NodeManifest] = new OneToMany[T, U]
}

object ManyToOne {
  def apply[T : NodeManifest, U : NodeManifest] = new ManyToOne[T, U]
}

class OneToMany[T : NodeManifest, U : NodeManifest] extends ToMany[T, U] {
  type ReverseRelationship = ManyToOne[U, T]

  def reverse = ManyToOne[U, T]
}

class ManyToOne[T : NodeManifest, U : NodeManifest] extends ToOne[T, U] {
  type ReverseRelationship = OneToMany[U, T]

  def reverse = OneToMany[U, T]
}

class Tree[T : NodeManifest] extends Relationship[T, T] {
  def tNodeManifest = NodeManifest[T]
  def uNodeManifest = NodeManifest[T]

  type ReverseRelationship = Tree[T]

  def reverse = this
}

case class RelationshipMappings(packagePrefix: String) {
  // TODO : thar be dragons
  def tagMap[T <: Relationship[_, _]](`class`: Class[T]): Map[Tag, List[Relationship[_, _]]] =
    new Reflections(packagePrefix).getSubTypesOf(`class`).toArray.toList
      .map { i =>
        val z: T = i.asInstanceOf[Class[T]].newInstance
        Map(
          z.tNodeManifest.tag -> List[Relationship[_, _]](z),
          z.uNodeManifest.tag -> List[Relationship[_, _]](z.reverse)) }
      .combineAll

  // TODO : investigating infer implicit
  // import reflect.macros._, scala.language.experimental.macros
  // def lookupImplicitImpl[T : c.WeakTypeTag](c: blackbox.Context): c.Expr[T] = { import c.mirror._; val t = c.inferImplicitValue(weakTypeOf[T]); println(t); c.Expr(t) }
  // def lookupImplicit[T]: T = macro lookupImplicitImpl[T]
  // scala> val z = lookupImplicit[Int]
  // <empty>
  // <empty>
  // z: Int = 0

  // TODO : this needs to be done for all relationship types
  lazy val mapping: Map[Tag, List[Relationship[_, _]]] =
    List(
      tagMap(classOf[OneToOne[_, _]]),
      tagMap(classOf[OneToMany[_, _]]),
      tagMap(classOf[ManyToOne[_, _]]),
      tagMap(classOf[Tree[_]])).combineAll

}

object Relationship {

  def apply[T : NodeManifest, U : NodeManifest : Relationship[T, ?]] = implicitly[Relationship[T, U]]

  implicit def reverseOneToOne[T : NodeManifest, U : NodeManifest](implicit ev: OneToOne[T, U]) = new OneToOne[U, T]

  implicit def reverseOneToMany[T : NodeManifest, U : NodeManifest](implicit ev: OneToMany[T, U]) = new ManyToOne[U, T]

  def relationship[T, U](implicit ev: Relationship[T, U]) = true

  def oneToOne[T, U](implicit ev: OneToOne[T, U]) = true

  def oneToMany[T, U](implicit ev: OneToMany[T, U]) = true

  def manyToOne[T, U](implicit ev: ManyToOne[T, U]) = true

}

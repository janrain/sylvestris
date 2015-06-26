package graph

import org.reflections.Reflections
import scalaz._, Scalaz._

sealed class Relationship[T : NodeManifest, U : NodeManifest] {
  val tNodeManifest = NodeManifest[T]
  val uNodeManifest = NodeManifest[U]
}

// trait

class OneToOne[T : NodeManifest, U : NodeManifest] extends Relationship[T, U]

class OneToMany[T : NodeManifest, U : NodeManifest] extends Relationship[T, U]

class ManyToOne[T : NodeManifest, U : NodeManifest] extends Relationship[T, U]

class Tree[T : NodeManifest] extends Relationship[T, T]

case class RelationshipMappings(packagePrefix: String) {
  // TODO this needs to be done for all relationship types
  val mapping: Map[Tag, List[Relationship[_, _]]] =
    new Reflections(packagePrefix).getSubTypesOf(classOf[OneToOne[_, _]]).toArray.toList
      .map { i =>
        val z = i.asInstanceOf[Class[OneToOne[_, _]]].newInstance
        Map(
          z.tNodeManifest.tag -> List[Relationship[_, _]](z),
          z.uNodeManifest.tag -> List[Relationship[_, _]](new OneToOne()(z.uNodeManifest, z.tNodeManifest)))
      }
      .suml
}

object Relationship {

  def apply[T : NodeManifest, U : NodeManifest : Relationship[T, ?]] = implicitly[Relationship[T, U]]

  implicit def reverseOneToOne[T : NodeManifest, U : NodeManifest](implicit ev: OneToOne[T, U]) = new OneToOne[U, T]

  implicit def reverseOneToMany[T : NodeManifest, U : NodeManifest](implicit ev: OneToMany[T, U]) = new ManyToOne[U, T]

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

package graph

object Validation {
  def apply[T : Validation] = implicitly[Validation[T]]

  implicit def fromManifest[T : NodeManifest]: Validation[T] = implicitly[NodeManifest[T]].validation
}

trait Validation[T] {
  // def validate(v: T): GraphM[T]
}

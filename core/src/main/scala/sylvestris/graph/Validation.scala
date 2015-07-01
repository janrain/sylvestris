package sylvestris.core

object Validation {

  case class Error(message: String)

  def apply[T : Validation] = implicitly[Validation[T]]

  implicit def fromManifest[T : NodeManifest]: Validation[T] = implicitly[NodeManifest[T]].validation
}

trait Validation[T] {
  // def validate(v: T): GraphM[List[Error]]
}

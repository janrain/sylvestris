package sylvestris.core

import scalaz.{ \/, EitherT }

object EitherTGraphM {

  def apply[T, U](v: T \/ U): EitherT[GraphM, T, U] = EitherT(GraphM(v))

  def apply[T, U](f: Graph => EitherT[GraphM, T, U]): EitherT[GraphM, T, U] =
    EitherT(GraphM(g => f(g).run.run(g)))

}

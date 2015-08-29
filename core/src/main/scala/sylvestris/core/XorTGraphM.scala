package sylvestris.core

import cats.data._

object XorTGraphM {

  def apply[T, U](v: T Xor U): XorT[GraphM, T, U] = XorT(GraphM(v))

  def apply[T, U](f: Graph => XorT[GraphM, T, U]): XorT[GraphM, T, U] =
    XorT(GraphM(g => f(g).value.run(g)))

}

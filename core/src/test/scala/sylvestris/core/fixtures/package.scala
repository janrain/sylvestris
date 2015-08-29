package sylvestris.core

import cats.data._

package object fixtures {

  def runAssertIsRight[T, U](g: Graph)(op: XorT[GraphM, T, U]): T Xor U = {
    val result = op.value.run(g)
    assert(result.isRight, result)
    result
  }

}

package sylvestris.core

import scalaz.{ \/, EitherT }

package object fixtures {

  def runAssertIsRight[T, U](g: Graph)(op: EitherT[GraphM, T, U]): T \/ U = {
    val result = op.run.run(g)
    assert(result.isRight, result)
    result
  }

}

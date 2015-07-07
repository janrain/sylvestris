package sylvestris.core

import scalaz.{ EitherT, Monad }

trait GraphM[T] {
  def run: Graph => T
  def map[U](f: T => U): GraphM[U] = GraphM(i => f(run(i)))
  def flatMap[U](f: T => GraphM[U]): GraphM[U] = GraphM(g => f(run(g)).run(g))
}

object GraphM {

  implicit object monadInstance extends Monad[GraphM] {
    def point[A](a: => A): GraphM[A] = new GraphM[A] { def run: Graph => A = g => a }
    def bind[A, B](fa: GraphM[A])(f: A => GraphM[B]): GraphM[B] = fa.flatMap(f)
  }

  def apply[T](v: Graph => T) = new GraphM[T] { def run: Graph => T = v }

  def apply[T](v: T) = new GraphM[T] { def run: Graph => T = _ => v }

}

package catsclaw

import cats._, data._
import cats.implicits._

object implicits {

  implicit def setMonoid[A]: Monoid[Set[A]] = MonoidK[Set].algebra[A]

  implicit def xorMonoid[A, B](implicit A: Semigroup[A], B: Monoid[B]): Monoid[A Xor B] =
    new Monoid[A Xor B] {
      def empty: A Xor B = Xor.Right(B.empty)
      def combine(x: A Xor B, y: A Xor B): A Xor B = x combine y
    }

  implicit class RichFoldable[A, F[_]](val v: F[A]) extends AnyVal {
    def combineAll(implicit M: Monoid[A], F: Foldable[F]): A = F.fold(v)
  }

}

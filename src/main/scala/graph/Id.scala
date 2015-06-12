package graph

import scalaz.Equal
import spray.json._

object Id {
  implicit def eqInstance[T] = Equal.equalA[Id[T]]

  trait IdJsonFormat {
    def writeId(v: String) = JsString(v)
    def readId(value: JsValue) = value match {
      case JsString(x) => x
      case x           => sys.error("Expected String as JsString, but got " + x)
    }
  }

  implicit val existentialIdJsonFormat: JsonFormat[Id[_]] = new JsonFormat[Id[_]] with IdJsonFormat {
    def write(x: Id[_]): JsValue = writeId(x.v)
    def read(value: JsValue) = Id(readId(value))
  }

  implicit def jsonFormat[T]: JsonFormat[Id[T]] = new JsonFormat[Id[T]] with IdJsonFormat {
    def write(x: Id[T]) = writeId(x.v)
    def read(value: JsValue) = Id[T](readId(value))
  }
}

case class Id[T](v: String)

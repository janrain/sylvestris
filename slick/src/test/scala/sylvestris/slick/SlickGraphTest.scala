package sylvestris.slick

import slick.driver.PostgresDriver.simple.Database
import sylvestris.core._

object SlickGraphTest extends GraphTest {
  def withGraph[T](f: Graph => T): T = {
    val db = Database.forURL(
      url = "jdbc:h2:mem:${java.util.UUID.randomUUID};DB_CLOSE_DELAY=-1",
      driver = "org.h2.Driver")

    db.withTransaction { implicit session => f(new SlickGraph) }
  }

}

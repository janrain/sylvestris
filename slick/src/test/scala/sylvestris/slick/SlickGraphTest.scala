package sylvestris.slick

import java.util.UUID
import slick.driver.PostgresDriver.simple.Database
import sylvestris.core._

object SlickGraphTest extends GraphTest {
  def withGraph[T](f: Graph => T): T = {
    val db = Database.forURL(
      url = s"jdbc:h2:mem:${UUID.randomUUID};DB_CLOSE_DELAY=-1",
      driver = "org.h2.Driver")

    db.withTransaction { implicit session => f(new SlickGraph) }
  }

}

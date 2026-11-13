package app.components

import org.scalatest.funspec.AsyncFunSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Future
import scala.concurrent.Promise

/** Covers PagedLoader's paging state and, above all, that a stale response can't overwrite a newer one. Assertions
  * chain off the same Promise the loader registered its callback on first, so they run after the loader's update.
  */
class PagedLoaderSpec extends AsyncFunSpec with Matchers {

  private def settled(p: Promise[?]): Future[Unit] = p.future.transformWith(_ => Future.unit)

  it("keeps the latest request's result when an earlier response arrives last") {
    val first = Promise[List[String]]()
    val second = Promise[List[String]]()
    val responses = Map("old" -> first, "new" -> second)
    val loader = PagedLoader[String, String]((_, q) => responses(q).future, Nil, initialQuery = "", pageSize = 2)

    loader.load(1, "old")
    loader.load(3, "new")
    second.success(List("new result"))

    settled(second).flatMap { _ =>
      first.success(List("stale", "stale"))
      settled(first).map { _ =>
        loader.itemsVar.now() shouldBe List("new result")
        loader.pageVar.now() shouldBe 3
        loader.activeQuery shouldBe "new"
        loader.hasNextVar.now() shouldBe false
        loader.loadingVar.now() shouldBe false
      }
    }
  }

  it("stays loading while the latest request is pending, even if an older one settles") {
    val first = Promise[List[String]]()
    val second = Promise[List[String]]()
    val responses = Map("old" -> first, "new" -> second)
    val loader = PagedLoader[String, String]((_, q) => responses(q).future, Nil, initialQuery = "")

    loader.load(1, "old")
    loader.load(1, "new")
    first.success(List("stale"))

    settled(first).map { _ =>
      loader.loadingVar.now() shouldBe true
      loader.itemsVar.now() shouldBe Nil
    }
  }

  it("offers a next page only after a full page, and pages through the last loaded query") {
    var calls = List.empty[(Int, String)]
    val loader = PagedLoader[String, String](
      (page, q) => { calls = calls :+ (page -> q); Future.successful(List("a", "b")) },
      Nil,
      initialQuery = "",
      pageSize = 2
    )

    loader.load(1, "q")
    Future.unit.flatMap(_ => Future.unit).map { _ =>
      loader.hasNextVar.now() shouldBe true
      loader.goToNextPage()
      calls shouldBe List(1 -> "q", 2 -> "q")
    }
  }
}

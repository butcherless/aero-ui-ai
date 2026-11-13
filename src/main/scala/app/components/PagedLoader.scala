package app.components

import app.api.Http
import com.raquo.laminar.api.L.*

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

/** Paginated list loading shared by every list page: fetches one page for a query, tracks loading/error state, and on
  * failure falls back to sample data only when the backend was unreachable (see `Http.loadFailure`). The backend sends
  * no total count, so "Next" is offered whenever the last fetch returned a full page.
  *
  * `Q` is whatever a page searches by: a plain query string for `EntityCrudPage`, an (origin, destination) pair for
  * Routes.
  */
final class PagedLoader[Q, T](
    fetchPage: (Int, Q) => Future[List[T]],
    sampleData: List[T],
    initialQuery: Q,
    pageSize: Int = Http.defaultPageSize
)(using ExecutionContext) {

  val itemsVar: Var[List[T]] = Var(List.empty[T])
  val loadingVar: Var[Boolean] = Var(true)
  val errorVar: Var[Option[String]] = Var(Option.empty[String])
  val pageVar: Var[Int] = Var(1)
  val hasNextVar: Var[Boolean] = Var(false)

  // The query of the last load, distinct from whatever is typed in a search box right now, so Prev/Next page through
  // what was actually searched.
  private val activeQueryVar: Var[Q] = Var(initialQuery)

  def activeQuery: Q = activeQueryVar.now()

  def load(page: Int, query: Q): Unit = {
    loadingVar.set(true)
    errorVar.set(None)
    fetchPage(page, query).onComplete { result =>
      loadingVar.set(false)
      pageVar.set(page)
      activeQueryVar.set(query)
      result match {
        case Success(list) =>
          hasNextVar.set(list.size >= pageSize)
          itemsVar.set(list)
        case Failure(ex) =>
          val failure = Http.loadFailure(ex)
          errorVar.set(Some(failure.message))
          hasNextVar.set(false)
          itemsVar.set(if (failure.useSampleData) sampleData else Nil)
      }
    }
  }

  def reload(): Unit = load(1, activeQuery)
  def goToPrevPage(): Unit = if (pageVar.now() > 1) load(pageVar.now() - 1, activeQuery)
  def goToNextPage(): Unit = if (hasNextVar.now()) load(pageVar.now() + 1, activeQuery)

  def paginationBar: HtmlElement = Pagination(pageVar.signal, hasNextVar.signal, goToPrevPage, goToNextPage)
}

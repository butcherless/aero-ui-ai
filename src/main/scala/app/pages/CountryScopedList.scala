package app.pages

import app.api.Http
import app.api.Http.given
import app.components.DebouncedFilterInput
import app.components.EntityListSpec
import com.raquo.laminar.api.L.*

import scala.concurrent.Future

/** The list behavior Airports and Airlines share. Both can be browsed by country code, searched by name, or listed, and
  * the backend has no endpoint combining a country with a name. So the country box takes priority when set, and
  * focusing either box clears the other.
  */
private[pages] object CountryScopedList {

  /** The backend's name search requires 3+ characters. */
  val MinNameSearchLength = 3

  // ISO 3166-1 alpha-2 codes are exactly 2 letters, so 2 is both the minimum and the practical maximum useful length.
  private val MinCountrySearchLength = 2

  /** The endpoints the list is built from. `search` returns every match unpaginated. */
  final case class Endpoints[T](
      list: Int => Future[List[T]],
      search: String => Future[List[T]],
      byCountry: (String, Int) => Future[List[T]]
  )

  private def fetch[T](endpoints: Endpoints[T], countryFilterVar: Var[String])(
      page: Int,
      query: String
  ): Future[List[T]] = {
    val country = countryFilterVar.now().trim.toUpperCase
    if (country.nonEmpty) endpoints.byCountry(country, page)
    else if (query.trim.length >= MinNameSearchLength)
      // Slice the unpaginated matches so Prev/Next behave as they do for a plain list.
      endpoints.search(query.trim).map(_.slice((page - 1) * Http.defaultPageSize, page * Http.defaultPageSize))
    else endpoints.list(page)
  }

  /** Call once per page instance, so the country filter's state doesn't outlive the page. */
  def listSpec[T](
      title: String,
      noun: String,
      columns: List[(String, T => String)],
      rowKey: T => String,
      matchesSearch: (T, String) => Boolean,
      sampleData: List[T],
      endpoints: Endpoints[T]
  ): EntityListSpec[T] = {
    val countryFilterVar = Var("")
    EntityListSpec[T](
      title = title,
      searchPlaceholder = s"Search $noun by name ($MinNameSearchLength+ characters)…",
      columns = columns,
      rowKey = rowKey,
      matchesSearch = matchesSearch,
      sampleData = sampleData,
      fetchPage = fetch(endpoints, countryFilterVar),
      serverSearch = true,
      minSearchLength = MinNameSearchLength,
      renderExtraToolbar = (reload, clearSearch) =>
        List(
          DebouncedFilterInput(countryFilterVar, "Country code (e.g. ES)", MinCountrySearchLength, reload, clearSearch)
        ),
      onSearchFocus = () => countryFilterVar.set("")
    )
  }
}

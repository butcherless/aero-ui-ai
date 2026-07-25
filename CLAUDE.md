# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Aviation admin panel (reactive SPA) built with **Scala 3 + Scala.js + Laminar**, served by **Vite**. The entities (Countries, Airports, Airlines, Aircraft, Flights, Flight Instances, Routes) and their fields are driven directly by an OpenAPI spec for an "Aviation Hexagonal API" backend at `http://localhost:8080`. UI text is in English.

**The backend has no CORS support** (no `Access-Control-Allow-Origin`, and its OPTIONS preflight returns 405), so `Http.baseUrl` is `""` (relative) and `vite.config.js` proxies `/api` to `http://localhost:8080` — this keeps every fetch same-origin from the browser's point of view. Don't "fix" this by pointing `baseUrl` at an absolute `http://localhost:8080` again; that reintroduces the CORS failure the proxy exists to avoid.

The backend's source lives in the sibling repo `../aero-hex-ai` ("aero-hex-ai"). Claude's role here is **frontend only**: that repo is **read-only** — look at it freely for reference (its OpenAPI spec, GitHub Actions workflows, sbt plugin/library versions, conventions) but never create, edit, or delete files inside it. Port anything useful back into this repo's own files instead.

## Commands

Development (two terminals):

```
sbt ~fastLinkJS              # compiles Scala.js in watch mode → public/js/main.js
npm install && npm run dev   # Vite dev server at http://localhost:5173
```

Production build:

```
sbt fullLinkJS && npm run build   # output in dist/
```

Tests (ScalaTest, run under Node+jsdom):

```
sbt test                                    # full suite
sbt "testOnly app.components.EntityCrudPageSpec"   # single spec
```

Format/lint:

```
sbt scalafmtAll scalafixAll              # apply
sbt scalafmtCheckAll "scalafixAll --check"   # CI-style check, no changes
```

**Always run `sbt scalafmtAll scalafixAll` and commit the result before pushing** — CI runs the check-only form (`scalafmtCheckAll "scalafixAll --check"`) and fails the build on any diff.

CI (`.github/workflows/ci.yml`) runs on every push/PR to `main`: format/lint check, `sbt test`, `sbt fullLinkJS`, then `npm run build`. Its action pins and hardening (SHA-pinned actions, `permissions`, `concurrency`, `sbt/setup-sbt`) mirror the sibling `aero-hex-ai` backend's own workflow conventions.

## Architecture

**Layering**, bottom to top:
- `api/Http.scala` — the only place that touches `dom.fetch`; wraps it with upickle JSON (de)serialization and turns non-2xx responses into `Http.ApiError`. `api/*Api.scala` — one thin module per entity (`CountriesApi`, `AirportsApi`, ...), each just building paths/query strings and delegating to `Http`.
- `models/Dtos.scala` — case classes matching the OpenAPI schemas exactly (`derives ReadWriter` via upickle). Note: `AirportDto`/`AirlineDto` do **not** include the country in GET responses even though it's required on create/update — that's the real API's shape, not a bug; `AirportsApi.countryOf` exists specifically to look it up separately.
- `components/` — generic, entity-agnostic UI pieces. **`EntityCrudPage`** is the important one: it's a generic list/search/select/create/edit page shell (paginated loading, sample-data fallback, client-side search filtering *within the current page*, row selection state) that a page instantiates with its columns, a `fetchPage: Int => Future[List[T]]`, and create/edit form renderers. `EntityTable` (the list), `FormField` (label+input widgets), `FormActions` (Save/Delete/Cancel/Close button rows), `Pagination` (Previous/Page N/Next), and `AsyncAction.run` (the flip-saving/clear-error/run-Future/report-result pattern) are the reusable building blocks `EntityCrudPage` and the pages are built from.
- **Pagination**: every `*Api.list()` fetches `pageSize=20` (`Http.defaultPageSize`) via `page`/`pageSize` query params — the backend caps `pageSize` at 100 and defaults to 20 itself. List responses carry no total count, so `EntityCrudPage`/`FlightInstancesPage` treat "Next" as enabled whenever the last fetch returned a full page (`items.size >= pageSize`); there's no way to know the real last page ahead of time.
- `pages/*Page.scala` — one per entity. **Countries, Airports, Airlines, Aircraft, and Flights are thin**: each just calls `EntityCrudPage[Dto](...)` with its own field lists and a small `editForm`/`createForm` pair. **Routes and Flight Instances are hand-rolled, not `EntityCrudPage`**, because their API contracts don't fit standard CRUD — see below.
- `router/AppRouter.scala` — minimal dependency-free router on the History API. Adding a page means: a `Page` case, a path mapping, a case in `App.scala`'s match, and a nav link in `Sidebar.scala`.

**Why Routes and Flight Instances are special-cased**: the backend has no list-all/get-by-key/update/delete endpoints for routes (only create, browse-by-operating-airline, and airline-association endpoints), and Flight Instances is read-only (no create/update/delete at all). Don't assume a uniform pattern across all 7 pages — check which shape a given entity's endpoints actually support before copying a pattern from another page. `FlightInstancesPage` duplicates `EntityCrudPage`'s pagination state (`page`/`hasNext` Vars, the `Pagination` component) by hand rather than sharing it, since it isn't built on `EntityCrudPage`; `RoutesPage` has no pagination at all — it's browsed by airline search, not a paginated list, so the pattern doesn't apply there.

**Sample-data fallback is intentional, not a bug**: every page tries the real backend first and falls back to a small hardcoded sample list with a visible `Http.backendUnreachableMessage` banner if the fetch fails. This keeps the app fully demoable without a running backend — don't "fix" this by removing the fallback or treating the banner as an error to suppress.

**Testing**: no official Laminar testing library exists; the suite mirrors Laminar's own (unpublished) test setup using ScalaTest + `com.raquo::domtestutils`, running under Node+jsdom (`scalajs-env-jsdom-nodejs`, requires the `jsdom` npm package). `testkit/LaminarMountSpec`/`LaminarAsyncMountSpec` mount real Laminar elements via Laminar's own `render(...)` (not domtestutils' raw `mount()`, which wouldn't activate Signal/Var reactivity) and assert directly against the DOM — domtestutils' richer `expectNode(...)` matcher DSL needs per-library glue that only exists in Laminar's unpublished internal test sources, so don't try to introduce it without re-deriving that glue. Concrete pages aren't tested directly (they call real `*Api` objects hitting `dom.fetch`, unreliable under jsdom) — only the injectable, dependency-free pieces (`EntityCrudPage`, `EntityTable`, `FormField`, DTOs, `Http.query`) are.

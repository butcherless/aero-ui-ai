# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Aviation admin panel (reactive SPA) built with **Scala 3 + Scala.js + Laminar**, served by **Vite**. The entities (Countries, Airports, Airlines, Aircraft, Flights, Flight Instances, Routes) and their fields are driven directly by an OpenAPI spec for an "Aviation Hexagonal API" backend at `http://localhost:8080`. UI text is in English.

The whole app is gated behind login (`api/v1/auth/login`); an unauthenticated visitor sees only the login screen. The backend has no roles/permissions module yet, so any valid token gets full CRUD — the read-only "View" pages under `/view/...` exist purely as a preview of a future restricted role, not real access control. See **Architecture** below for how the auth and read-only pieces fit together.

**The backend has no CORS support** (no `Access-Control-Allow-Origin`, and its OPTIONS preflight returns 405), so `Http.baseUrl` is `""` (relative) and `vite.config.js` proxies `/api` to `http://localhost:8080` — this keeps every fetch same-origin from the browser's point of view. Don't "fix" this by pointing `baseUrl` at an absolute `http://localhost:8080` again; that reintroduces the CORS failure the proxy exists to avoid.

The backend's source lives in the sibling repo `../aero-hex-ai` ("aero-hex-ai"). Claude's role here is **frontend only**: that repo is **read-only** — look at it freely for reference (its OpenAPI spec, GitHub Actions workflows, sbt plugin/library versions, conventions) but never create, edit, or delete files inside it. Port anything useful back into this repo's own files instead.

## Commands

Development (two terminals):

```
sbt ~fastLinkJS              # compiles Scala.js in watch mode → public/js/main.js
npm install && npm run dev   # Vite dev server at http://localhost:5173
```

Or background both with `scripts/start-dev.sh` (logs to `.dev-run/*.log`); stop with `scripts/stop-dev.sh`.

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
- `api/Http.scala` — the only place that touches `dom.fetch`; wraps it with upickle JSON (de)serialization and turns non-2xx responses into `Http.ApiError`. It also attaches `Authorization: Bearer <token>` from `Session.token` to every request, and centrally handles session expiry: any non-2xx **401** on a path other than `/api/v1/auth/login` itself clears the session and redirects to the login page, since that means the token is missing/expired/invalid rather than a wrong-password attempt. `api/*Api.scala` — one thin module per entity (`CountriesApi`, `AirportsApi`, ...) plus `AuthApi` (login/logout), each just building paths/query strings and delegating to `Http`.
- `auth/Session.scala` — the client-side session (bearer token, expiry, username), persisted to `localStorage` so a page refresh doesn't force a fresh login before the token's natural expiry. `App.scala` hard-gates the entire UI on `Session.isAuthenticated`: `false` renders the standalone `LoginPage` (no sidebar/`Layout`), `true` renders `Layout` with the normal page content. `TopBar` (inside `Layout`) shows the logged-in user, a "Disconnect" button (clears the session client-side immediately; the server-side logout call is best-effort and never blocks on it), and an exact-code quick-lookup widget (country/airport/airline by code, independent of the per-page name search).
- `models/Dtos.scala` — case classes matching the OpenAPI schemas exactly (`derives ReadWriter` via upickle). Note: `AirportDto`/`AirlineDto` do **not** include the country in GET responses even though it's required on create/update — that's the real API's shape, not a bug; `AirportsApi.countryOf` exists specifically to look it up separately.
- `components/` — generic, entity-agnostic UI pieces. **`MasterDetailShell`** is the outer shell every page renders into: a title, a list panel (toolbar + table), and a detail panel that only mounts when something's selected. **`EntityCrudPage`** builds on it and is the important one: a generic list/search/select/create/edit page shell (paginated loading, sample-data fallback, client-side search filtering *within the current page*, row selection state) that a page instantiates with its columns, a `fetchPage: Int => Future[List[T]]`, and create/edit form renderers; `EntityCrudPage.readOnly` is the read-only counterpart used by the `/view/...` pages (see below). `EntityTable` (the list), `FormField` (label+input widgets, plus `FormField.readOnly` for static field dumps), `FormActions` (Save/Delete/Cancel/Close button rows), `Pagination` (Previous/Page N/Next), `DebouncedFilterInput` (the shared secondary-filter box — Airports'/Airlines' country filter, Aircraft's airline filter — distinct from `EntityCrudPage`'s own built-in debounced search box), and `AsyncAction.run` (the flip-saving/clear-error/run-Future/report-result pattern) are the reusable building blocks `EntityCrudPage` and the pages are built from.
- **Pagination**: every `*Api.list()` fetches `pageSize=20` (`Http.defaultPageSize`) via `page`/`pageSize` query params — the backend caps `pageSize` at 100 and defaults to 20 itself. List responses carry no total count, so `EntityCrudPage`/`FlightInstancesPage` treat "Next" as enabled whenever the last fetch returned a full page (`items.size >= pageSize`); there's no way to know the real last page ahead of time.
- `pages/*Page.scala` — one per entity, plus `LoginPage` (standalone, shown instead of `Layout` while unauthenticated) and `ProfilePage` (shows the logged-in identity and links to the read-only preview pages). **Countries, Airports, Airlines, Aircraft, and Flights are thin**: each just calls `EntityCrudPage[Dto](...)` with its own field lists and a small `editForm`/`createForm` pair. **Routes and Flight Instances are hand-rolled, not `EntityCrudPage`**, because their API contracts don't fit standard CRUD — see below.
- **Read-only "View" pages**: since the backend has no roles/permissions module yet, any valid token can do full CRUD. `Page.ViewCountries`/`ViewAirports`/`ViewAirlines`/`ViewAircraft`/`ViewFlights`/`ViewRoutes` (routes under `/view/...`, linked from `ProfilePage`) render `EntityCrudPage.readOnly`/`RoutesPage.readOnly` instead of the writable page — same data, pagination, and search, but no "+ Add" and no edit/delete. Flight Instances is already read-only, so it has no separate `/view` variant.
- `router/AppRouter.scala` — minimal dependency-free router on the History API. Adding a page means: a `Page` case, a path mapping, a case in `App.scala`'s match, and a nav link in `Sidebar.scala`.

**Why Routes and Flight Instances are special-cased**: the backend has no list-all/get-by-key/update/delete endpoints for routes (only create, browse-by-operating-airline, and airline-association endpoints), and Flight Instances is read-only (no create/update/delete at all). Don't assume a uniform pattern across all 7 pages — check which shape a given entity's endpoints actually support before copying a pattern from another page. `FlightInstancesPage` duplicates `EntityCrudPage`'s pagination state (`page`/`hasNext` Vars, the `Pagination` component) by hand rather than sharing it, since it isn't built on `EntityCrudPage`; `RoutesPage` has no pagination at all — it's browsed by airline search, not a paginated list, so the pattern doesn't apply there.

**Sample-data fallback is intentional, not a bug**: every page tries the real backend first and falls back to a small hardcoded sample list with a visible `Http.backendUnreachableMessage` banner if the fetch fails. This keeps the app fully demoable without a running backend — don't "fix" this by removing the fallback or treating the banner as an error to suppress.

**Testing**: no official Laminar testing library exists; the suite mirrors Laminar's own (unpublished) test setup using ScalaTest + `com.raquo::domtestutils`, running under Node+jsdom (`scalajs-env-jsdom-nodejs`, requires the `jsdom` npm package). `testkit/LaminarMountSpec`/`LaminarAsyncMountSpec` mount real Laminar elements via Laminar's own `render(...)` (not domtestutils' raw `mount()`, which wouldn't activate Signal/Var reactivity) and assert directly against the DOM — domtestutils' richer `expectNode(...)` matcher DSL needs per-library glue that only exists in Laminar's unpublished internal test sources, so don't try to introduce it without re-deriving that glue. Concrete entity pages aren't tested directly (they call real `*Api` objects hitting `dom.fetch`, unreliable under jsdom) — only the injectable, dependency-free pieces (`EntityCrudPage`, `EntityTable`, `FormField`, DTOs, `Http.query`) are. `Session` and `AppRouter` are tested directly since they're pure state with no network calls; `TopBar` is tested too even though it calls `AuthApi.logout`, because its Disconnect handler clears the session synchronously regardless of whether that best-effort call succeeds.

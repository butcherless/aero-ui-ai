# CLAUDE.md

## Project

Aviation admin panel SPA: **Scala 3 + Scala.js + Laminar**, served by **Vite**. Entities (Countries, Airports, Airlines, Aircraft, Flights, Flight Instances, Routes) and their fields follow the OpenAPI spec of the backend at `http://localhost:8080`. UI text is in English.

The backend's source is the sibling repo `../aero-hex-ai`. **That repo is read-only from here**: use it for reference (OpenAPI spec, workflows, versions, conventions), but never change files in it. Port anything useful into this repo.

The whole app sits behind login (`/api/v1/auth/login`). The backend has no roles yet, so any valid token gets full CRUD; the `/view/...` pages only preview a future restricted role and are not access control.

**The backend has no CORS support**, so `Http.baseUrl` is `""` and `vite.config.js` proxies `/api` to `http://localhost:8080`. Don't point `baseUrl` at an absolute URL; that brings the CORS failure back.

## Commands

```
sbt ~fastLinkJS                              # dev: compile in watch mode → public/js/main.js
npm install && npm run dev                   # dev: Vite at http://localhost:5173
sbt fullLinkJS && npm run build              # production build → dist/
sbt test                                     # full suite (ScalaTest under Node+jsdom)
sbt "testOnly app.components.EntityCrudPageSpec"   # single spec
sbt scalafmtAll scalafixAll                  # format and lint
sbt scalafmtCheckAll "scalafixAll --check"   # same check as CI
```

`scripts/start-dev.sh` runs both dev processes in the background (logs in `.dev-run/*.log`); `scripts/stop-dev.sh` stops them.

**Always run `sbt scalafmtAll scalafixAll` and commit the result before pushing**, because CI fails on any diff.

## CI

- `.github/workflows/ci.yml` runs on every push/PR to `main`: format/lint check, `sbt test`, `sbt fullLinkJS`, `npm run build`. Its conventions mirror `aero-hex-ai`.
- `.github/workflows/dependency-updates.yml` reports outdated dependencies on Fridays at 15:00 UTC or on a manual trigger, and changes nothing. `scripts/check_plugin_updates.py` and `scripts/check_actions_updates.py` are copies of the scripts in `aero-hex-ai`'s `bump-versions` skill.

## Architecture

Under `src/main/scala/app/`, bottom to top:

- **`api/Http.scala`** is the only code touching `dom.fetch`. It handles upickle (de)serialization, turns non-2xx into `Http.ApiError`, and attaches the bearer token from `Session.token`. A 401 on any path except login clears the session and redirects to login. **`api/*Api.scala`** has one thin module per entity plus `AuthApi`.
- **`auth/Session.scala`** holds the token, expiry and username, persisted to `localStorage`. `App.scala` gates the UI on `Session.isAuthenticated`: standalone `LoginPage` if false, `Layout` if true. `TopBar` shows the user, an exact-code lookup widget, and "Disconnect", which clears the session immediately (the server logout is best-effort and never blocks).
- **`models/Dtos.scala`** has case classes matching the OpenAPI schemas exactly. `AirportDto`/`AirlineDto` omit the country in GET responses even though create/update requires it. That's the real API shape, so `AirportsApi.countryOf` looks it up separately.
- **`components/`** holds generic UI pieces:
  - `MasterDetailShell`: the outer shell of every page (title, list panel, detail panel).
  - `EntityCrudPage`: the main one, a generic list/search/select/create/edit page, with `EntityCrudPage.readOnly` for `/view/...`. Its search filters client-side *within the current page*.
  - `EntityTable`, `FormField`, `FormActions`, `Pagination`, `AsyncAction.run`, and `DebouncedFilterInput` (the secondary filter box, distinct from `EntityCrudPage`'s own search).
- **`pages/*Page.scala`** has one page per entity, plus `LoginPage` and `ProfilePage` (links to the read-only previews). Countries, Airports, Airlines, Aircraft and Flights are thin `EntityCrudPage[Dto](...)` calls.
- **`router/AppRouter.scala`** is a dependency-free History API router. A new page needs a `Page` case, a path mapping, a case in `App.scala`, and a link in `Sidebar.scala`.

**Pagination:** `*Api.list()` fetches `Http.defaultPageSize` (20) per page; the backend caps `pageSize` at 100. Responses have no total count, so "Next" is enabled whenever the last fetch returned a full page.

**Read-only pages:** `Page.ViewCountries`/`ViewAirports`/`ViewAirlines`/`ViewAircraft`/`ViewFlights`/`ViewRoutes` render `EntityCrudPage.readOnly`/`RoutesPage.readOnly`. Flight Instances is already read-only, so it has no `/view` variant.

**Routes and Flight Instances are hand-rolled, not `EntityCrudPage`:** Routes has create, list/search and airline-association endpoints, but no get-by-key/update/delete. Flight Instances is read-only. Check which endpoints an entity supports before copying a pattern from another page. Both pages duplicate the pagination state by hand. `RoutesPage` has two origin/destination filters usable together, because that endpoint supports combining them.

**The sample-data fallback is intentional:** each page tries the backend, then falls back to hardcoded samples with a visible `Http.backendUnreachableMessage` banner, so the app can be demoed without a backend. Don't remove it or suppress the banner.

## Testing

There's no official Laminar testing library, so the suite mirrors Laminar's unpublished setup with ScalaTest + `com.raquo::domtestutils` under Node+jsdom.

- `testkit/LaminarMountSpec`/`LaminarAsyncMountSpec` mount elements with Laminar's `render(...)`, not domtestutils' `mount()`, which doesn't activate Signal/Var reactivity.
- Don't introduce domtestutils' `expectNode(...)`: it needs glue that only exists in Laminar's unpublished test sources.
- Concrete entity pages aren't tested because `dom.fetch` is unreliable under jsdom. Test the injectable pieces instead: `EntityCrudPage`, `EntityTable`, `FormField`, DTOs, `Http.query`, `Session`, `AppRouter`. `TopBar` is tested too, because Disconnect clears the session synchronously.

# aero-ui-ai

Aviation admin panel (reactive SPA) built with **Scala 3 + Scala.js + Laminar**, driven by the `Aviation Hexagonal API` OpenAPI spec.

## Structure

```
build.sbt                              # dependencies and Scala.js config
project/                               # sbt-scalajs plugin + sbt version
src/main/scala/app/
  App.scala                            # entry point
  router/AppRouter.scala                # lightweight History API-based routing (no external deps)
  components/Layout.scala               # overall layout (sidebar + content)
  components/Sidebar.scala              # entity navigation
  components/MasterDetailShell.scala    # shared list + detail-panel page shell
  components/EntityCrudPage.scala       # generic list/search/select/create/edit page shell (built on the two below)
  components/EntityTable.scala          # generic searchable/selectable list table
  components/Pagination.scala           # Previous/Page N/Next bar for paginated lists
  components/FormField.scala            # label+input building blocks for detail/create forms
  components/FormActions.scala          # shared Save/Delete/Cancel/Close button rows
  components/AsyncAction.scala          # shared "run a Future, track saving/error state" helper
  api/Http.scala                        # fetch + upickle JSON client wrapper
  api/*Api.scala                        # one thin API module per entity
  models/Dtos.scala                     # case classes matching the OpenAPI schemas
  pages/*Page.scala                     # one master-detail page per entity
public/style.css                        # base styles
index.html                              # served by Vite
src/test/scala/app/
  testkit/                              # shared test base classes (mount Laminar elements into jsdom)
  models/DtosSpec.scala                 # JSON round-trip / Option-handling tests
  api/HttpSpec.scala                    # pure query-string helper tests
  components/*Spec.scala                # component + EntityCrudPage state-machine tests
```

Each entity page (Countries, Airports, Airlines, Aircraft, Flights, Flight Instances, Routes) tries the real backend first and falls back to sample data with a visible banner if it's unreachable, so the UI is fully demoable without a live backend. List pages fetch 20 items per page with Previous/Next pagination.

The backend itself has no CORS support, so `vite.config.js` proxies `/api` requests to `http://localhost:8080` and the app talks to that proxy (same-origin) rather than the backend directly — see "Running in development" below.

## Requirements

- JDK 17+
- sbt
- Node.js ^20.19 or >=22.12 (required by Vite 8)

## Running in development

Terminal 1 — compiles Scala.js in watch mode, writing to `public/js/main.js`:

```
sbt ~fastLinkJS
```

Terminal 2 — installs JS dependencies and starts Vite:

```
npm install
npm run dev
```

Open http://localhost:5173 — every Scala code change recompiles (terminal 1) and Vite reloads the page automatically.

For the app to show real data, the backend must be running at `http://localhost:8080`; Vite's dev-server proxy forwards `/api/*` there (see `vite.config.js`) so browser requests stay same-origin, since the backend itself doesn't send CORS headers. Without a reachable backend, each page falls back to sample data with a banner.

## Production build

```
sbt fullLinkJS
npm run build
```

Output lands in `dist/`.

## Tests

```
sbt test
```

Tests run under Node + jsdom (via `scalajs-env-jsdom-nodejs`, requires the `jsdom` npm package — already in `package.json`). The suite uses **ScalaTest** + **`com.raquo::domtestutils`**, mirroring Laminar's own test setup (there's no separate official Laminar testing library). `testkit/LaminarMountSpec` / `LaminarAsyncMountSpec` mount real Laminar elements via `render(...)` (not a raw `appendChild`) so Signal/Var-driven bindings actually activate, then assert directly against the rendered DOM — `domtestutils`'s richer `expectNode(...)` matcher DSL needs framework-specific glue that only exists in Laminar's own unpublished test sources.

## Code style

```
sbt scalafmtAll        # format
sbt scalafixAll         # apply lint rules (unused imports, import ordering)
sbt scalafmtCheckAll    # CI-style check, no changes
sbt "scalafixAll --check"
```

## Continuous Integration

`.github/workflows/ci.yml` runs on every push/PR to `main`: format/lint check, `sbt test`, `sbt fullLinkJS`, then `npm run build`.

## Suggested next steps

- **More advanced routing**: if you need route params or query strings, swap `AppRouter` for [Waypoint](https://github.com/raquo/Waypoint) (same family as Laminar, same author).
- **Consuming existing JS libraries** (charts, grids, date pickers): use [ScalablyTyped](https://scalablytyped.org/) to generate typed facades automatically from their TypeScript types.
- **Sharing DTOs with a Scala backend**: use `sbt-crossproject` to cross-compile the `models` package for both JVM and Scala.js instead of hand-maintaining two copies.

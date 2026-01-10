# VN Admin Data — Copilot instructions for code changes

This short guide tells an AI coding agent how to be productive in this repository quickly.
Refer to `docs/` for BRD/architecture if you need higher-level context.

Key context
- Purpose: Spring Boot toolset for extracting/matching Vietnamese administrative boundaries and UBND points from GADM and OSM, storing results in H2 or PostGIS, and serving a minimal Leaflet-based UI (`src/main/resources/static`).
- Major components:
  - `vn.admin.web.MapApplication` — Spring Boot entrypoint and `JtsModule` registration (`src/main/java/vn/admin/web/MapApplication.java`).
  - `vn.admin.service.DataImporter` — GeoJSON ingestion + SQL updates (see `importGadmData()` and `updateUpperLevels()`).
  - `vn.admin.service.MapService` — SQL-first GeoJSON builders and helpers (returns `JsonNode` from SQL built with `jsonb_build_object` / `ST_AsGeoJSON`).
  - `vn.admin.controller.MapController` — REST routes under `/api/map` (controllers call `MapService`).

Quick start (commands & environment)
- Build: `./gradlew build`
- Run app (H2 default): `./gradlew bootRun`
- Dev profile (enables frontend dev redirect): `./gradlew bootRunDev`
- Run tests: `./gradlew test` (JUnit Platform + Serenity/Cucumber feature runner).
- Run a single test class: `./gradlew test --tests "vn.admin.MapApiIntegrationTest"`
- Postgres env: set `DB_URL`, `DB_USER`, `DB_PASSWORD` (or let Testcontainers provide credentials for tests).
- Note: project uses Java 17 toolchain — CI workflows should target Java 17 (tooling in `build.gradle`).

Project conventions & patterns (concrete)
- SQL-first: Most logic for geometry and GeoJSON lives in SQL inside `MapService` / `DataImporter`. When changing output shape, modify SQL first and verify returned JSON string is valid GeoJSON.
  - Example: `MapService` uses `ST_AsGeoJSON(geom_boundary)::jsonb` and `jsonb_build_object(...)` to build features.
  - Important: Use `ST_AsGeoJSON(geom_boundary)` directly; a comment in `MapService` warns **do NOT use `ST_Envelope`** for geometry output.
- Ingestion patterns (DataImporter): use `ST_Multi(ST_GeomFromText(...))` or `ST_GeomFromGeoJSON(...)`, `ON CONFLICT DO NOTHING` for idempotent inserts, and `updateUpperLevels()` to call `ST_Union` and set `geom_label = ST_PointOnSurface(geom_boundary)`.
- Table naming: all app tables use `vn_` prefix (`vn_provinces`, `vn_districts`, `vn_wards`) — avoid renaming these without careful migration.
- Serialization: `MapApplication` registers `JtsModule` so prefer returning JTS geometry objects when possible and rely on Jackson for serialization.

Testing notes & examples
- Unit tests: `src/test/java/vn/admin/service/MapServiceUnitTest.java` shows mocking `JdbcTemplate` and reflectively injecting it into `MapService` for fast unit tests.
- Integration tests: `src/test/java/vn/admin/MapApiIntegrationTest.java` uses Testcontainers with a PostGIS image (`postgis/postgis:15-3.3`) declared as compatible with `postgres`. Tests manually create extensions (`CREATE EXTENSION postgis`) and minimal schema, insert a simple `MULTIPOLYGON` via `ST_Multi(ST_GeomFromText(...))`, and assert GeoJSON payloads.
- Test outputs: Serenity/Cucumber feature outcomes are placed under `build/serenity` and copied to `target/site/serenity` by the Gradle configuration.
- Optional Playwright integration: this repo supports an opt-in Playwright driver via `net.serenity-bdd:serenity-playwright` and a convenience task `installPlaywrightBrowsers`. See `docs/PLAYWRIGHT.md` for details and CI install snippets.

Developer tips & debugging
- If testing SQL changes, validate against a PostGIS instance (local Docker Compose or Testcontainers) — PostGIS-specific behavior is the source of many subtle bugs.
- To quickly debug serialization errors, increase logging for `org.springframework.web` (see `MapApiIntegrationTest` for how tests set logging during runs).
- Use `jdbcTemplate.queryForObject("SELECT ST_AsGeoJSON(geom_boundary) FROM vn_provinces WHERE province_id = ?", String.class, id)` in small ad-hoc checks.

What not to do
- Don’t change `vn_` table names or embedded SQL strings without adding explicit DB migrations and tests.
- Don’t commit large data files (`data/`, OSM PBFs, or `*.mv.db` for H2); these are intentionally ignored.

Where to look (quick links)
- SQL & API: `src/main/java/vn/admin/service/MapService.java` and `src/main/java/vn/admin/controller/MapController.java`
- Import code: `src/main/java/vn/admin/service/DataImporter.java`
- App entry & JTS: `src/main/java/vn/admin/web/MapApplication.java`
- Integration tests: `src/test/java/vn/admin/MapApiIntegrationTest.java`
- Unit tests & mocking example: `src/test/java/vn/admin/service/MapServiceUnitTest.java`
- Feature tests (Serenity/Cucumber): `src/test/resources/features`
- Operational guide & osm2pgsql examples: `README.md`

Suggested first tasks for an AI agent
- Add a GitHub Actions workflow that runs `./gradlew build` and `./gradlew test` on push (matrix: Java 17).
- Add a `docker-compose.yml` for Postgres+PostGIS + sample init SQL and document how to use it in `README.md`.
- Add/expand integration tests for `MapService` edge cases (use Testcontainers + PostGIS image `postgis/postgis:15-3.3`).

If anything is unclear or you want me to update a specific section (e.g., CI workflow template, example `docker-compose.yml`, or test skeletons), tell me which piece and I’ll iterate.

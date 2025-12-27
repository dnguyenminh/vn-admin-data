## VN Admin Data — Copilot instructions for code changes

This file gives concise, actionable information for an AI coding agent to be productive in this repository.

Key context
- Purpose: a small Java/Spring Boot toolset to extract and match Vietnamese administrative boundaries and UBND points from GADM and OSM, store results in H2 or PostGIS, and serve a minimal Leaflet web UI.
- Major components:
  - `vn.admin.web.MapApplication` — Spring Boot entrypoint (see `src/main/java/vn/admin/web/MapApplication.java`)
  - `vn.admin.service.DataImporter` — GADM GeoJSON ingestion and PostGIS SQL updates (`importGadmData` method)
  - `vn.admin.service.MapService` — SQL-first GeoJSON builders that return raw JSON strings via `JdbcTemplate`
  - `vn.admin.controller.MapController` — REST endpoints under `/api/map`
  - DB: supports H2 (local developer flow) and PostgreSQL/PostGIS (production-like spatial queries)

Getting started (commands & env)
- Build: `./gradlew build`
- Run entire app (default H2): `./gradlew run`
- Run web only: `./gradlew :web:bootRun` or `./gradlew bootRun` depending on how you want to start (README has examples).
- DB env (for Postgres): set `DB_URL`, `DB_USER`, `DB_PASSWORD` before starting. Example: `export DB_URL=jdbc:postgresql://localhost:5432/gis`
- Big-data import: `osm2pgsql` is used outside Java to load PBF into PostGIS (see README examples).

Project-specific conventions & patterns
- SQL-first design: Many API responses are composed by PostGIS SQL (e.g., `ST_AsGeoJSON`, `ST_PointOnSurface`, `ST_Union`) inside `MapService` and `DataImporter`. Prefer modifying SQL in the service for shape/precision changes.
- Table naming: application tables use `vn_` prefixes (`vn_provinces`, `vn_districts`, `vn_wards`) — search for these names when adding migrations or queries.
- JSON: API endpoints often return raw JSON strings built from SQL (`jdbcTemplate.queryForObject(sql, String.class)`). If changing output shape, change SQL first and ensure string remains valid GeoJSON.
- Geometry handling: `DataImporter` stores GeoJSON via `ST_GeomFromGeoJSON(?)` and uses `ST_Multi(...)` and `ST_PointOnSurface(...)` — follow this pattern when ingesting geometry.
- Serialization: `MapApplication` registers the `JtsModule` bean so geometry types are serialized by Jackson — when returning geometry objects (instead of raw JSON) prefer JTS types and rely on `JtsModule`.

Developer workflows & debugging tips
- Local H2 DB: default path referenced in README (`./data/vn_admin_db`); the local H2 DB file `vn_admin_db.mv.db` is ignored by `.gitignore` and should remain local.
- If testing SQL changes, prefer running against a PostGIS instance (Docker Compose or local) to validate spatial functions. Consider using Testcontainers in tests.
- There are currently no unit tests; add focused tests for `MapService` or `DataImporter` that use a Postgres+PostGIS test DB.

Integration points & external deps
- PostGIS/Postgres for spatial queries and heavy imports. The README documents `osm2pgsql` commands used to import the full PBF.
- OSM PBF and GADM GeoJSON (large files) are NOT tracked — avoid committing to `data/` (it's ignored). Use Git LFS or artifact storage if needed.

Safe change examples (how to implement common edits)
- Add a new GeoJSON endpoint for provinces:
  1. Implement SQL in `MapService.getProvinceGeoJson(...)` using `ST_AsGeoJSON(geom_boundary)` and `jsonb_build_object`.
  2. Add a controller method in `MapController` mapping to `/api/map/provinces/geojson`.
  3. Add an integration test that runs the query against PostGIS Testcontainer.
- Change import behavior to also populate `area_km2`: update `DataImporter.updateUpperLevels()` to compute areas with `ST_Area(...::geography)/1e6` and persist to `vn_provinces`/`vn_districts`.

What not to do
- Do not change table names arbitrarily — many SQL strings are embedded directly in services.
- Do not commit `data/` or `*.mv.db` files; they are intentionally ignored to prevent huge pushes and secrets leakage.

Where to look for examples
- `src/main/java/vn/admin/service/MapService.java` — examples of SQL-driven GeoJSON builders.
- `src/main/java/vn/admin/service/DataImporter.java` — pattern for ingesting GeoJSON into PostGIS.
- `src/main/java/vn/admin/web/MapApplication.java` — JTS module registration for geometry JSON support.
- `README.md` — operational instructions: `osm2pgsql` examples, PostGIS tips, and web UI usage.

Suggested first tasks for an AI agent
1. Add a simple GitHub Action workflow that runs `./gradlew build` and `./gradlew test` on push to `main` (use matrix Java 21).
2. Add integration tests (using Testcontainers) for `MapService` to validate GeoJSON SQL outputs.
3. Add `docker-compose.yml` to spin up Postgres+PostGIS for local development and update README with exact commands.

If anything above is unclear or you'd like expansion (examples for a specific change like adding a new endpoint or tests), ask and I'll iterate.

*** Helpful note: This file was generated by a repository audit; please review for omissions or project-specific constraints. ***

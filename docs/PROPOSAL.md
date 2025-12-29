# Proposed improvements & next steps

This document lists prioritized, concrete changes and tests to improve reliability, performance, and maintainability.

## Goals
- Ensure safe data import and DB integrity
- Improve search performance
- Make API responses consistent and testable
- Add integration tests for PostGIS-backed queries
- Prepare safe steps for running DataImporter

## Short checklist
- [ ] Add DB indexes for search and spatial queries
- [ ] Harden DataImporter (batching, transactions, backup)
- [ ] Return typed JSON (DTO / ResponseEntity) from geo endpoints
- [ ] Add integration tests (Testcontainers Postgres+PostGIS)
- [ ] Add CORS / dev proxy guidance for frontend/backends
- [ ] Add docs for running importer and restoring backups

## Concrete changes (implementation notes)

1) DB indexes (recommended SQL)
```sql
-- Enable trigram extension once
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Speed up ILIKE '%...%' searches on name_vn
CREATE INDEX IF NOT EXISTS idx_provinces_name_trgm ON vn_provinces USING gin (name_vn gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_districts_name_trgm ON vn_districts USING gin (name_vn gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_wards_name_trgm ON vn_wards USING gin (name_vn gin_trgm_ops);

-- Spatial GIST indexes for geometry-based queries
CREATE INDEX IF NOT EXISTS gist_vn_provinces_geom ON vn_provinces USING gist (geom_boundary);
CREATE INDEX IF NOT EXISTS gist_vn_districts_geom ON vn_districts USING gist (geom_boundary);
CREATE INDEX IF NOT EXISTS gist_vn_wards_geom ON vn_wards USING gist (geom_boundary);
```

2) DataImporter hardening
- Wrap each file import in a DB transaction and batch INSERTs to reduce IO.
- Before running importer: create DB backup or copy affected tables to backup_*. Example in SQL:
```sql
CREATE TABLE backup_vn_wards AS TABLE vn_wards;
CREATE TABLE backup_vn_districts AS TABLE vn_districts;
CREATE TABLE backup_vn_provinces AS TABLE vn_provinces;
```
- Add optional `dryRun` flag to DataImporter to validate JSON parsing and SQL without committing.

3) API typing improvements
- Replace controller methods that return String (raw JSON) with ResponseEntity<JsonNode> or a DTO.
  - Let MapService return JsonNode (use Jackson) or Map, not raw String.
  - Example change outline (MapService): parse SQL result into com.fasterxml.jackson.databind.JsonNode and return.
- Use consistent HTTP status codes on error.

4) Search performance
- Add trigram GIN indexes (see #1).
- Consider limiting `UNION ALL` ordering or adding relevance-based ranking in SQL.

5) Integration tests
- Add tests using Testcontainers (Postgres image with PostGIS).
- Create a test that:
  - Starts container
  - Seeds minimal data (1 province, 1 district, 1 ward)
  - Calls the /api/map endpoints and asserts valid GeoJSON and lists

6) Frontend dev notes
- useGisApi already contains logic for SSR vs dev proxy. Document running:
  - Backend: ./gradlew bootRun (or `./gradlew :bootRun`), default port 8080
  - Frontend: cd web && npm install && npm run dev
- Document CORS if running separately (spring config or dev proxy Nuxt).

## Commands to run locally (safe, non-destructive)
- Run backend:
```bash
./gradlew bootRun
```
- Run frontend (from `web/`):
```bash
cd web && npm ci && npm run dev
```

## Next immediate steps I can implement
- Create migration SQL file adding the recommended indexes (non-destructive).
- Add a small integration test skeleton using Testcontainers.
- Modify MapService/controller to return JsonNode instead of raw String (small PR).
- Add a `dryRun` option to DataImporter.

# vn-admin-data

A small Java toolset to extract and match Vietnamese admin boundaries and UBND points from GADM and OpenStreetMap (OSM).

This repository contains:
- App.java — main Java application that:
  - scans OSM PBF to collect UBND/administrative points and assemblies
  - processes GADM GeoJSON to match provinces/districts/wards and populate an H2 database
  - computes and stores UBND and center coordinates (and boundary GeoJSON) for admin units
- h2/H2Launcher.java — small launcher for an H2 TCP server (configurable via env vars)

This README documents how to run locally, how to import full OSM data into PostGIS, and common example queries.

---

## Quick start (development)

Prereqs:

Run the app (default uses an H2 TCP database at `./data/vn_admin_db`):

```bash
./gradlew run
```

Run the app in development mode (enables dev-only features):

```bash
# Option A: use the convenience Gradle task
./gradlew bootRunDev

# Option B: set Spring profile to `dev`
./gradlew bootRun -Dspring.profiles.active=dev
```

**Dev profile note:** when running with the `dev` profile (`-Dspring.profiles.active=dev` or `SPRING_PROFILES_ACTIVE=dev`), Flyway is configured to run `baselineOnMigrate=true` for local convenience so existing local databases without a `flyway_schema_history` table will be baselined automatically. This makes `bootRun` easier to start against a pre-populated development database; do not use this setting for production databases without understanding the implications.

What it does:
  - `level1_provinces(name, ubnd_lat, ubnd_lon, center_lat, center_lon, geom_geojson)`
  - `level2_districts(tinh, name, ubnd_lat, ubnd_lon, center_lat, center_lon, geom_geojson)`
  - `administrative_units(id, tinh, huyen, xa, area_km2, ubnd_lat, ubnd_lon, center_lat, center_lon, geom_geojson)`

 - New: synthetic "contiguous" display — Voronoi-derived polygons are available as a visual aid when district geometries are disjoint. Use the web UI toggle "Show Contiguous (synthetic)" on a province to render side-by-side district regions for visualization (non-destructive).


## Import full OSM into PostGIS (recommended for spatial queries)

This repository includes helper instructions to import the full PBF into a PostGIS database using `osm2pgsql`.

1. Install PostGIS on host or use Docker (recommended if you prefer isolated environment).

2. Create `gis` DB and enable extensions:

```bash
sudo -u postgres createdb gis
sudo -u postgres psql -d gis -c "CREATE EXTENSION postgis; CREATE EXTENSION hstore;"
```

3. Install `osm2pgsql` and run import (example tuned for ~32GB system):

```bash
# install osm2pgsql (Debian/Ubuntu)
sudo apt update && sudo apt install -y osm2pgsql

# import
PGPASSWORD=postgres osm2pgsql --slim --hstore -C 12000 --number-processes 8 -d gis -U postgres -H localhost -S /usr/share/osm2pgsql/default.style data/vietnam-251222.osm.pbf
```

Result tables (created by osm2pgsql):
- `planet_osm_point` (points)
- `planet_osm_line` (lines)
- `planet_osm_polygon` (polygons)
- `planet_osm_roads`

Use PostGIS spatial queries to find UBND points, boundaries, and proximity tests. See "Example queries" below.

---

## Example SQL queries

Connect with psql:

```bash
sudo -u postgres psql -d gis
```

Get overall counts:
```sql
SELECT (SELECT count(*) FROM planet_osm_point) AS points,
       (SELECT count(*) FROM planet_osm_line) AS lines,
       (SELECT count(*) FROM planet_osm_polygon) AS polygons;
```

Find UBND-like points (amenity tags or name contains ubnd):
```sql
SELECT osm_id, name, ST_AsGeoJSON(way) AS geojson
FROM planet_osm_point
WHERE (tags->'amenity' = 'townhall'
       OR name ILIKE '%ubnd%'
       OR name ILIKE '%ủy ban%')
LIMIT 50;
```

Find polygon for a province (e.g., Hà Nội) and export GeoJSON:
```sql
COPY (
  SELECT json_build_object(
    'type','FeatureCollection', 'features', json_agg(json_build_object('type','Feature','geometry', ST_AsGeoJSON(way)::json, 'properties', json_build_object('name', name, 'osm_id', osm_id)))
  )
  FROM planet_osm_polygon
  WHERE name = 'Hà Nội' AND (admin_level='4' OR boundary='administrative')
) TO '/tmp/hanoi.geojson';
```

Find UBND points inside Hà Nội polygon (uses ST_Contains):
```sql
WITH h AS (
  SELECT way FROM planet_osm_polygon WHERE name='Hà Nội' AND (admin_level='4' OR boundary='administrative') LIMIT 1
)
SELECT n.osm_id, n.name, ST_AsGeoJSON(n.way) AS geojson
FROM planet_osm_point n, h
WHERE ST_Contains(h.way, n.way)
  AND (n.tags->'amenity' = 'townhall' OR n.name ILIKE '%ubnd%');
```

Compute area (km²) using geography:
```sql
SELECT name, ST_Area(way::geography)/1e6 AS area_km2
FROM planet_osm_polygon
WHERE name='Hà Nội';
```

---

### Using the application's PostGIS tables (level1_provinces, level2_districts, administrative_units)

The Java app now creates `geom` (PostGIS geometry) columns when running against PostgreSQL/PostGIS and populates them from the stored GeoJSON. Below are a few handy queries to verify and use these columns.

Verify geometry population and counts:
```sql
SELECT 'level1_provinces' AS table, COUNT(*) AS total, COUNT(geom) FILTER (WHERE geom IS NOT NULL) AS geom_nonnull FROM level1_provinces
UNION ALL
SELECT 'level2_districts', COUNT(*), COUNT(geom) FILTER (WHERE geom IS NOT NULL) FROM level2_districts
UNION ALL
SELECT 'administrative_units', COUNT(*), COUNT(geom) FILTER (WHERE geom IS NOT NULL) FROM administrative_units;
```

Find which province contains a given point (lon, lat):
```sql
-- Replace <lon> and <lat> with coordinates
SELECT name FROM level1_provinces
WHERE ST_Contains(geom, ST_SetSRID(ST_MakePoint(<lon>, <lat>), 4326));
```

List wards inside a district (spatial join):
```sql
SELECT a.id, a.tinh, a.huyen, a.xa
FROM administrative_units a
JOIN level2_districts d ON ST_Contains(d.geom, a.geom)
WHERE d.name ILIKE '%<district name>%';
```

Compute accurate area (km²) from the geometry and store/update `area_km2`:
```sql
-- Geodesic area (recommended)
UPDATE administrative_units SET area_km2 = ST_Area(geom::geography)/1e6;

-- Verify a few rows
SELECT id, area_km2 FROM administrative_units ORDER BY area_km2 DESC LIMIT 5;
```

Ensure spatial indexes (the app creates GIST indexes automatically, but you can create/verify):
```sql
CREATE INDEX IF NOT EXISTS idx_administrative_units_geom ON administrative_units USING GIST (geom);
CREATE INDEX IF NOT EXISTS idx_level1_provinces_geom ON level1_provinces USING GIST (geom);
CREATE INDEX IF NOT EXISTS idx_level2_districts_geom ON level2_districts USING GIST (geom);

-- List indexes
\di
```

Export a single administrative unit geometry as GeoJSON:
```sql
COPY (
  SELECT json_build_object('type','FeatureCollection', 'features', json_agg(json_build_object('type','Feature','geometry', ST_AsGeoJSON(geom)::json, 'properties', json_build_object('id', id, 'name', xa))))
  FROM administrative_units WHERE id = '<GID_3>'
) TO '/tmp/unit_<GID_3>.geojson';
```

---

## Tính diện tích (area) và cập nhật vào H2


## Database performance: customer listing

If you observe slow `/api/map/customers` calls (especially for substring searches), there is a migration that creates a small `customers` table with indexes to provide fast, deduplicated listings and fast search.

See `scripts/migrations/001_create_customers.sql` and `scripts/migrations/README.md` for details on applying the migration and validation steps.

Nếu bạn muốn tính diện tích các đơn vị hành chính (tỉnh/huyện) và **cập nhật** giá trị vào H2 (`level1_provinces`/`level2_districts`), làm theo các bước sau:

- Tính diện tích theo ellipsoid (chính xác) trong PostGIS và xuất ra CSV:

```bash
sudo -u postgres psql -d gis -c "COPY (
  SELECT name, ST_Area(way::geography)/1e6 AS area_km2
  FROM planet_osm_polygon
  WHERE admin_level='4'
) TO '/tmp/province_areas.csv' CSV HEADER;"
```

- Tạo bảng tạm và cập nhật vào H2 (dùng H2 shell / JDBC). Ví dụ dùng H2 Shell:

```bash
java -cp ~/.gradle/caches/.../h2-*.jar org.h2.tools.Shell \
  -url "jdbc:h2:tcp://127.0.0.1:9092/./vn_admin_db" -user sa \
  -sql "CREATE TABLE tmp_province_area AS SELECT * FROM CSVREAD('/tmp/province_areas.csv');"

# Trong H2 (psql-like):
UPDATE level1_provinces p SET area_km2 = (
  SELECT a.area_km2 FROM tmp_province_area a WHERE a.name = p.name
);
```

Lưu ý:
- Tên (`name`) giữa OSM và GADM có thể không khớp hoàn toàn — hãy kiểm tra bảng `tmp_province_area` trước khi cập nhật và cân nhắc sử dụng chỉ số GADM nếu có thể. ⚠️
- Bạn có thể tính tương tự cho `admin_level='6'` (huyện) và xuất CSV cho `level2_districts`.

Tip: Nếu muốn chính xác hơn khi join tên, có thể dùng các hàm fuzzy matching (ví dụ pg_trgm trên Postgres) hoặc xuất thêm thông tin geo + kiểm tra bằng không gian (ví dụ ST_Contains) để liên kết tự động.

---

## H2 usage & App-specific queries

If you prefer the Java app outputs stored in H2 (the app creates these tables):
- Connect to H2 TCP: JDBC `jdbc:h2:tcp://localhost/./data/vn_admin_db` (user `sa`, no password)
- Inspect results:

```sql
SELECT name, ubnd_lat, ubnd_lon, center_lat, center_lon FROM level1_provinces;
SELECT tinh, name, ubnd_lat, ubnd_lon, center_lat, center_lon FROM level2_districts;
SELECT id, tinh, huyen, xa, ubnd_lat, ubnd_lon, center_lat, center_lon FROM administrative_units LIMIT 10;
```

You can join `planet_osm_*` (PostGIS) with the H2 data by doing spatial queries in PostGIS and updating H2 if necessary.

---

## Troubleshooting & tips

- If PostGIS extensions aren't available, install `postgis` package for your PostgreSQL version.
- If `osm2pgsql` fails due to RAM, reduce `-C` or use `--flat-nodes` option to use disk. Increase `--number-processes` for more cores.
- For production, consider `imposm` or a managed database if scale grows.

---

## Testing & Serenity BDD (Cucumber)

This project uses the Cucumber JUnit Platform engine together with the
`serenity-cucumber` adapter to produce Serenity outcomes and HTML reports.

Key points:
- Use the published Serenity adapter that matches the Serenity core version (this project uses `net.serenity-bdd:serenity-cucumber:5.1.0`).
- Make the Cucumber JUnit Platform engine available at test runtime: `io.cucumber:cucumber-junit-platform-engine:7.x`.
- Register the Serenity reporter implementation provided by the `serenity-cucumber` adapter in `src/test/resources/cucumber.properties`:

```properties
# Register the Serenity reporter for the Cucumber JUnit Platform engine
cucumber.plugin=net.serenitybdd.cucumber.core.plugin.SerenityReporter
```

Migration note:
- The project previously used a legacy `@RunWith(CucumberWithSerenity.class)` runner. We now prefer a pure JUnit Platform approach and have removed the runner to keep the test setup modern and simple.

To run the acceptance tests and generate a Serenity report:

```bash
./gradlew test -Dserenity.outputDirectory=build/serenity --info
```

After the run, inspect the Serenity report and summary:

```bash
open build/serenity/index.html  # or cat build/serenity/summary.txt
```

---

## Next steps I can take (pick any)
- Add `docker-compose.yml` to spin up PostGIS + import service. ✅
- Export per-province GeoJSON files into `data/geojson/` for QA. ✅
- Add sample scripts and convenience SQL files into `scripts/`. ✅

---

If you'd like, I'll now:
- add the README to the repo (done), and
- add a `scripts/import-postgis.sh` helper and/or Docker Compose file next (tell me which).

Let me know which of the "Next steps" you'd like me to do first.
-- Backup current province geometries
DROP TABLE IF EXISTS backup_level1_provinces;
CREATE TABLE backup_level1_provinces AS
SELECT name, geom, geom_geojson FROM level1_provinces;

-- Create a table of unions per province
DROP TABLE IF EXISTS tmp_district_unions;
CREATE TABLE tmp_district_unions AS
SELECT tinh AS name, ST_Union(geom) AS union_geom, (ST_Area(ST_Union(geom)::geography)/1e6)::numeric(12,3) AS union_area_km2
FROM level2_districts
GROUP BY tinh;

-- Find mismatches where union is significantly larger than stored province geometry
DROP TABLE IF EXISTS tmp_province_mismatch;
CREATE TABLE tmp_province_mismatch AS
SELECT p.name, (ST_Area(p.geom::geography)/1e6)::numeric(12,3) AS stored_area_km2, u.union_area_km2, (u.union_area_km2 - (ST_Area(p.geom::geography)/1e6))::numeric(12,3) AS diff_km2
FROM level1_provinces p
JOIN tmp_district_unions u ON lower(p.name) = lower(u.name)
WHERE u.union_area_km2 > (ST_Area(p.geom::geography)/1e6) * 1.2 OR (u.union_area_km2 - (ST_Area(p.geom::geography)/1e6)) > 1.0;

-- Report mismatches count
SELECT COUNT(*) AS mismatch_count FROM tmp_province_mismatch;

-- Create a log table for updates
DROP TABLE IF EXISTS province_geom_updates;
CREATE TABLE province_geom_updates (name VARCHAR(100), old_area_km2 numeric(12,3), new_area_km2 numeric(12,3), updated_at TIMESTAMP DEFAULT now());

-- Perform updates: replace level1_provinces.geom and geom_geojson with union where mismatch criteria hold
WITH unions AS (
  SELECT tinh AS name, ST_Union(geom) AS union_geom, (ST_Area(ST_Union(geom)::geography)/1e6)::numeric(12,3) AS union_area_km2
  FROM level2_districts
  GROUP BY tinh
)
UPDATE level1_provinces p
SET geom = u.union_geom, geom_geojson = ST_AsGeoJSON(u.union_geom)
FROM unions u
WHERE lower(p.name) = lower(u.name)
  AND (u.union_area_km2 > (ST_Area(p.geom::geography)/1e6) * 1.2 OR (u.union_area_km2 - (ST_Area(p.geom::geography)/1e6)) > 1.0)
RETURNING p.name, (ST_Area((SELECT geom FROM backup_level1_provinces b WHERE lower(b.name)=lower(p.name))::geography)/1e6)::numeric(12,3) AS old_area_km2, u.union_area_km2;

-- Insert update records into log
INSERT INTO province_geom_updates (name, old_area_km2, new_area_km2)
SELECT p.name, (ST_Area((SELECT geom FROM backup_level1_provinces b WHERE lower(b.name)=lower(p.name))::geography)/1e6)::numeric(12,3) AS old_area_km2, u.union_area_km2
FROM level1_provinces p
JOIN unions u ON lower(p.name)=lower(u.name)
WHERE u.union_area_km2 > (ST_Area((SELECT geom FROM backup_level1_provinces b WHERE lower(b.name)=lower(p.name))::geography)/1e6) * 1.2 OR (u.union_area_km2 - (ST_Area((SELECT geom FROM backup_level1_provinces b WHERE lower(b.name)=lower(p.name))::geography)/1e6)) > 1.0;

-- Show summary of updates
SELECT * FROM province_geom_updates ORDER BY new_area_km2 - old_area_km2 DESC LIMIT 100;

-- Final checks: ensure AnGiang area updated
SELECT name, (ST_Area(geom::geography)/1e6)::numeric(12,3) AS area_km2 FROM level1_provinces WHERE name ILIKE 'AnGiang' OR name ILIKE 'An Giang';

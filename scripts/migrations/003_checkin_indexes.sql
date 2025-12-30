-- Migration: indexes to speed checkins queries
-- 1) Index for geojson queries: supports WHERE appl_id = ? AND field_lat/long IS NOT NULL ORDER BY id LIMIT ...
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_checkin_applid_id_nonnull_loc ON checkin_address (appl_id, id) WHERE field_lat IS NOT NULL AND field_long IS NOT NULL;

-- 2) Index to speed distinct fc_id listing and COUNT(DISTINCT fc_id) per appl
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_checkin_applid_fcid ON checkin_address (appl_id, fc_id);

-- Note: run this file with psql separately since CREATE INDEX CONCURRENTLY cannot run inside a transaction block.
-- Example:
-- psql 'postgresql://user:pass@host:5432/gis' -f scripts/migrations/003_checkin_indexes.sql

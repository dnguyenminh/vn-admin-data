-- Enable pg_trgm extension for efficient LIKE/ILIKE searches
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Create standard B-Tree index for sorting and exact lookups
CREATE INDEX IF NOT EXISTS idx_customers_appl_btree ON customers (appl_id);

-- Create GIN Trigram index for ILIKE '%...%' searches
-- Note: 'CONCURRENTLY' is removed because Flyway runs migrations in a transaction by default.
-- For production systems with zero-downtime requirements, consider running index creation manually or configuring Flyway to run non-transactionally.
CREATE INDEX IF NOT EXISTS idx_customers_appl_trgm ON customers USING gin (appl_id gin_trgm_ops);

-- Checkins indexes
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_tables WHERE tablename = 'checkin_address') THEN
    CREATE INDEX IF NOT EXISTS idx_checkin_applid_id_nonnull_loc ON checkin_address (appl_id, id) WHERE field_lat IS NOT NULL AND field_long IS NOT NULL;
    CREATE INDEX IF NOT EXISTS idx_checkin_applid_fcid ON checkin_address (appl_id, fc_id);
  END IF;
END
$$;

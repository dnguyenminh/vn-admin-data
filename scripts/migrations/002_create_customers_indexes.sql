-- Migration step 2: create indexes (run separately; CREATE INDEX CONCURRENTLY
-- cannot be executed inside a transaction block)

-- Optional: create pg_trgm extension for trigram index (requires superuser)
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Create a btree index for ordering & keyset on customers.appl_id
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_customers_appl_btree ON customers (appl_id);

-- Create a trigram GIN index to accelerate ILIKE '%q%' searches
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_customers_appl_trgm ON customers USING gin (appl_id gin_trgm_ops);

-- Note: run this file as a separate psql command, e.g.: 
-- psql 'postgresql://user:pass@host:5432/gis' -f scripts/migrations/002_create_customers_indexes.sql

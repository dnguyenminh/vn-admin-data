-- Migration: create customers table for fast distinct customer listing
-- NOTE: Creating the pg_trgm extension requires superuser privileges.

-- create deduplicated customers table
CREATE TABLE IF NOT EXISTS customers (
  appl_id text PRIMARY KEY
);

-- populate from existing customer_address
INSERT INTO customers (appl_id)
  SELECT DISTINCT appl_id FROM customer_address WHERE appl_id IS NOT NULL
  ON CONFLICT DO NOTHING;

-- btree index for ordering and keyset
-- Indexes are created in a separate migration (002_create_customers_indexes.sql)

-- trigger function to keep customers table roughly in sync with customer_address
CREATE OR REPLACE FUNCTION customers_upsert() RETURNS trigger AS $$
BEGIN
  IF (TG_OP = 'INSERT' OR TG_OP = 'UPDATE') THEN
    IF (NEW.appl_id IS NOT NULL) THEN
      INSERT INTO customers(appl_id) VALUES (NEW.appl_id) ON CONFLICT DO NOTHING;
    END IF;
    RETURN NEW;
  ELSIF (TG_OP = 'DELETE') THEN
    -- remove only when no more addresses reference the appl_id
    DELETE FROM customers WHERE appl_id = OLD.appl_id
      AND NOT EXISTS (SELECT 1 FROM customer_address WHERE appl_id = OLD.appl_id);
    RETURN OLD;
  END IF;
  RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_customers_upsert ON customer_address;
CREATE TRIGGER trg_customers_upsert
  AFTER INSERT OR UPDATE OR DELETE ON customer_address
  FOR EACH ROW EXECUTE FUNCTION customers_upsert();

-- NOTE: This script intentionally does not use an explicit transaction so
-- that large operations and subsequent concurrent index creations can be
-- executed separately. Run `002_create_customers_indexes.sql` afterwards to
-- create CONCURRENTLY indexes and optional pg_trgm extension.

-- Usage notes:
-- 1) Run this migration on your Postgres instance. If you cannot create the pg_trgm extension,
--    remove or defer the CREATE EXTENSION and TRGM index statements; the table and btree index
--    still provide major benefit for keyset/ordering and COUNT operations.

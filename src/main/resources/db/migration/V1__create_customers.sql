-- Flyway V1: create customers table and trigger (transactional)
CREATE TABLE IF NOT EXISTS customers (
  appl_id text PRIMARY KEY
);

-- Only populate if the source table exists (tests use an empty DB)
DO $$
  BEGIN
    IF to_regclass('public.customer_address') IS NOT NULL THEN
      INSERT INTO customers (appl_id)
        SELECT DISTINCT appl_id FROM customer_address WHERE appl_id IS NOT NULL
        ON CONFLICT DO NOTHING;
    END IF;
  END$$;

CREATE OR REPLACE FUNCTION customers_upsert() RETURNS trigger AS $$
BEGIN
  IF (TG_OP = 'INSERT' OR TG_OP = 'UPDATE') THEN
    IF (NEW.appl_id IS NOT NULL) THEN
      INSERT INTO customers(appl_id) VALUES (NEW.appl_id) ON CONFLICT DO NOTHING;
    END IF;
    RETURN NEW;
  ELSIF (TG_OP = 'DELETE') THEN
    DELETE FROM customers WHERE appl_id = OLD.appl_id
      AND NOT EXISTS (SELECT 1 FROM customer_address WHERE appl_id = OLD.appl_id);
    RETURN OLD;
  END IF;
  RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Only create trigger if the referenced table exists (avoids failure in clean test DBs)
DO $$
BEGIN
  IF to_regclass('public.customer_address') IS NOT NULL THEN
    DROP TRIGGER IF EXISTS trg_customers_upsert ON customer_address;
    CREATE TRIGGER trg_customers_upsert
      AFTER INSERT OR UPDATE OR DELETE ON customer_address
      FOR EACH ROW EXECUTE FUNCTION customers_upsert();
  END IF;
END$$;

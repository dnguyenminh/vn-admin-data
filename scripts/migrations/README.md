Migration: customers table
--------------------------

Purpose
-------
Create a small, deduplicated `customers` table to accelerate customer listing queries (no DISTINCT / COUNT on the large `customer_address` table).

How to run
----------
1. Inspect the SQL: `scripts/migrations/001_create_customers.sql`
2. Run the SQL on your Postgres instance as a user with appropriate privileges. Example using psql:

   ```bash
   psql "postgresql://user:pass@host:5432/db" -f scripts/migrations/001_create_customers.sql
   ```

3. Create indexes (run separately):

   ```bash
   psql "postgresql://user:pass@host:5432/db" -f scripts/migrations/002_create_customers_indexes.sql
   ```

   - If you cannot create the `pg_trgm` extension (superuser required), you can still run the script after removing the `CREATE EXTENSION` and trigram index lines; the btree index will still provide improvements for ordering and keyset queries.

Notes
-----
- The migration includes `CREATE EXTENSION IF NOT EXISTS pg_trgm;` which requires superuser privileges. If you cannot create the extension, you can remove or skip the extension and GIN index steps and still benefit from the customers table and btree index.
- The migration also creates a trigger `trg_customers_upsert` to keep the `customers` table roughly in sync with `customer_address`. If you prefer a periodic refresh instead, replace the trigger with a scheduled `REFRESH`/`INSERT ... SELECT DISTINCT` job.
- After applying the migration, the server will automatically prefer the `customers` table for listing/searching customers; no application restart is required (the change is immediate).

Validation
----------
Re-run the EXPLAINs used previously (see `scripts/explain_queries.sh`) and compare timings. You should see significant improvements for: `COUNT(DISTINCT ...)`, `DISTINCT ... ILIKE '%q%'`, and keyset scans.

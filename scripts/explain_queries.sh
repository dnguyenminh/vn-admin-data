#!/usr/bin/env bash
set -euo pipefail

# explain_queries.sh
# Usage:
#   DB_URL=postgresql://user:pass@host:5432/db ./scripts/explain_queries.sh [search_term] [outdir]
# If DB_URL is not set, psql default connection parameters (PGHOST/PGUSER etc.) are used.

DB_URL=jdbc:postgresql://postgres:postgres@localhost:5432/gis

# psql accepts libpq connection strings like postgresql://user:pass@host:port/db
# If DB_URL was provided as a JDBC URL (jdbc:...), strip the jdbc: prefix.
if [[ -n "${DB_URL:-}" && "${DB_URL}" == jdbc:* ]]; then
  echo "Normalizing JDBC URL to libpq format"
  DB_URL="${DB_URL#jdbc:}"
fi

SEARCH=${1:-foo}
OUTDIR=${2:-explain_outputs}
mkdir -p "$OUTDIR"

command -v psql >/dev/null 2>&1 || { echo "psql is required but not installed or not in PATH" >&2; exit 1; }

run() {
  local sql="$1"
  local out="$2"
  echo "Running query, saving to $out"
  if [ -n "${DB_URL:-}" ]; then
    psql "$DB_URL" -X -q -t -A -c "$sql" > "$out"
  else
    psql -X -q -t -A -c "$sql" > "$out"
  fi
}

# Refresh planner stats
if [ -n "${DB_URL:-}" ]; then
  echo "Running ANALYZE customer_address against $DB_URL"
  psql "$DB_URL" -X -q -c "ANALYZE customer_address;"
else
  echo "Running ANALYZE customer_address against default psql connection"
  psql -X -q -c "ANALYZE customer_address;"
fi

# Run EXPLAINs
run "EXPLAIN (ANALYZE, BUFFERS, VERBOSE, FORMAT JSON) SELECT COUNT(*) FROM (SELECT DISTINCT appl_id FROM customer_address WHERE appl_id ILIKE '%${SEARCH}%') t;" "$OUTDIR/explain_count_distinct.json"
run "EXPLAIN (ANALYZE, BUFFERS, VERBOSE, FORMAT JSON) SELECT DISTINCT appl_id FROM customer_address WHERE appl_id ILIKE '%${SEARCH}%' ORDER BY appl_id LIMIT 50;" "$OUTDIR/explain_distinct_limit_search.json"
run "EXPLAIN (ANALYZE, BUFFERS, VERBOSE, FORMAT JSON) SELECT DISTINCT appl_id FROM customer_address WHERE appl_id IS NOT NULL ORDER BY appl_id LIMIT 50;" "$OUTDIR/explain_distinct_limit_nosearch.json"
run "EXPLAIN (ANALYZE, BUFFERS, VERBOSE, FORMAT JSON) SELECT DISTINCT appl_id FROM customer_address WHERE appl_id IS NOT NULL ORDER BY appl_id LIMIT 50 OFFSET 10000;" "$OUTDIR/explain_offset_10000.json" || true
run "EXPLAIN (ANALYZE, BUFFERS, VERBOSE, FORMAT JSON) SELECT DISTINCT appl_id FROM customer_address WHERE appl_id > 'last-appl-id' ORDER BY appl_id LIMIT 50;" "$OUTDIR/explain_keyset.json" || true

echo "All queries finished. Outputs saved in $OUTDIR"
ls -lh "$OUTDIR"

echo "Done"

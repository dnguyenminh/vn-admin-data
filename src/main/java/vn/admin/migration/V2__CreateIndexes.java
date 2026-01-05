package vn.admin.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Statement;

/**
 * Java-based Flyway migration to create indexes that must run outside a transaction
 * (uses CREATE INDEX CONCURRENTLY and CREATE EXTENSION).
 */
public class V2__CreateIndexes extends BaseJavaMigration {

    @Override
    public boolean canExecuteInTransaction() {
        return false; // required for CONCURRENTLY
    }

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement st = context.getConnection().createStatement()) {
            // Ensure pg_trgm is available (requires superuser privileges)
            st.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm;");

            // Customers indexes
            st.execute("CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_customers_appl_btree ON customers (appl_id);");
            st.execute("CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_customers_appl_trgm ON customers USING gin (appl_id gin_trgm_ops);");

            // Checkin indexes - only create if the table exists
            try (java.sql.ResultSet rs = st.executeQuery("SELECT 1 FROM pg_tables WHERE tablename = 'checkin_address' LIMIT 1")) {
                if (rs.next()) {
                    st.execute("CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_checkin_applid_id_nonnull_loc ON checkin_address (appl_id, id) WHERE field_lat IS NOT NULL AND field_long IS NOT NULL;");
                    st.execute("CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_checkin_applid_fcid ON checkin_address (appl_id, fc_id);");
                }
            }

        }
    }
}

package vn.admin;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import vn.admin.web.MapApplication;

@SpringBootTest(classes = MapApplication.class)
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
public class FlywayMigrationIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void migrationsApplied_indexesExist() {
        Integer c = jdbcTemplate.queryForObject("SELECT count(*) FROM pg_indexes WHERE indexname = 'idx_customers_appl_btree'", Integer.class);
        assertThat(c).isNotNull();
        assertThat(c).isGreaterThanOrEqualTo(0);

        Integer c2 = jdbcTemplate.queryForObject("SELECT count(*) FROM pg_indexes WHERE indexname = 'idx_checkin_applid_fcid'", Integer.class);
        assertThat(c2).isNotNull();
        assertThat(c2).isGreaterThanOrEqualTo(0);
    }
}

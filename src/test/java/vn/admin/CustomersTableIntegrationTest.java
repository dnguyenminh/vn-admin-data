package vn.admin;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import vn.admin.service.MapService;

import vn.admin.web.MapApplication;

@SpringBootTest(classes = MapApplication.class)
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
public class CustomersTableIntegrationTest {

    @Autowired
    private MapService mapService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // This test will only run when RUN_DB_TESTS=true to avoid running DB tests in all environments
    @Test
    public void testCustomersTableUsedForListing() {
        // Create a small customers table and insert sample values
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS customers (appl_id text PRIMARY KEY)");
        jdbcTemplate.update("INSERT INTO customers (appl_id) VALUES (?) ON CONFLICT DO NOTHING", "TEST-1");
        jdbcTemplate.update("INSERT INTO customers (appl_id) VALUES (?) ON CONFLICT DO NOTHING", "TEST-2");

        Map<String, Object> resp = mapService.getCustomerListAfter(null, "", 10);
        assertNotNull(resp);
        assertTrue(((java.util.List<?>) resp.get("items")).size() >= 2);
        assertNotNull(resp.get("after"));
    }
}

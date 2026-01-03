package vn.admin;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import vn.admin.web.MapApplication;
import vn.admin.service.MapService;

@SpringBootTest(classes = MapApplication.class)
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
@TestPropertySource(properties = { "app.customersFirstPageCacheTtlMs=1000" })
public class CustomersCacheIntegrationTest {

    @Autowired
    private MapService mapService;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Test
    public void testFirstPageCacheExpires() throws Exception {
        // Ensure test starts clean
        jdbcTemplate.update("DELETE FROM customers WHERE appl_id = ?", "CACHE-TEST");
        mapService.clearCustomerFirstPageCache();

        // Prime cache
        Map<String, Object> first = mapService.getCustomerListAfter(null, "", 50);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) first.get("items");
        assertNotNull(items);

        // Insert a new customer row that should not be visible until cache expires
        jdbcTemplate.update("INSERT INTO customers (appl_id) VALUES (?) ON CONFLICT DO NOTHING", "CACHE-TEST");

        // Immediate fetch should still return cached data (not include CACHE-TEST)
        Map<String, Object> second = mapService.getCustomerListAfter(null, "", 50);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items2 = (List<Map<String, Object>>) second.get("items");
        boolean foundNow = items2.stream().anyMatch(m -> "CACHE-TEST".equals(m.get("id")));
        assertFalse(foundNow, "New customer should not be visible while cache is fresh");

        // Wait for TTL to expire (configured via TestPropertySource to 1s)
        Thread.sleep(1200);

        Map<String, Object> third = mapService.getCustomerListAfter(null, "", 50);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items3 = (List<Map<String, Object>>) third.get("items");
        boolean foundAfter = items3.stream().anyMatch(m -> "CACHE-TEST".equals(m.get("id")));
        assertTrue(foundAfter, "New customer should be visible after cache expiry");
    }
}

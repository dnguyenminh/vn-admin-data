package vn.admin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = vn.admin.web.MapApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@org.junit.jupiter.api.Disabled("Requires Docker/PostGIS; enable when Docker available")
public class MapApiIntegrationTest {

    // Use a PostGIS-enabled image. Adjust tag if needed.
    @org.testcontainers.junit.jupiter.Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgis/postgis:15-3.3")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void setDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // Let Hibernate auto-ddl be disabled for this test; we'll create minimal schema manually
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @LocalServerPort
    int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void beforeAll() {
        // ensure extension exists; will be executed after container starts by JDBC calls below in test setup
    }

    @Test
    void smokeTestApiEndpoints() throws Exception {
        // Create PostGIS extension and minimal tables
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS postgis");
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS postgis_topology");

        // Minimal schema compatible with the service queries
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS vn_provinces (
              province_id VARCHAR PRIMARY KEY,
              name_vn VARCHAR,
              geom_boundary geometry(MultiPolygon,4326)
            );
            """);

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS vn_districts (
              district_id VARCHAR PRIMARY KEY,
              province_id VARCHAR,
              name_vn VARCHAR,
              geom_boundary geometry(MultiPolygon,4326)
            );
            """);

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS vn_wards (
              ward_id VARCHAR PRIMARY KEY,
              district_id VARCHAR,
              name_vn VARCHAR,
              geom_boundary geometry(MultiPolygon,4326)
            );
            """);

        // Insert a simple polygon (small square) as MULTIPOLYGON covering a tiny area
        String polyWkt = "MULTIPOLYGON((((-0.001 0.0, -0.001 0.001, 0.001 0.001, 0.001 0.0, -0.001 0.0))))";
        jdbcTemplate.update("INSERT INTO vn_provinces (province_id, name_vn, geom_boundary) VALUES (?, ?, ST_Multi(ST_GeomFromText(?, 4326))) ON CONFLICT DO NOTHING", "P1", "Province One", polyWkt);
        jdbcTemplate.update("INSERT INTO vn_districts (district_id, province_id, name_vn, geom_boundary) VALUES (?, ?, ?, ST_Multi(ST_GeomFromText(?, 4326))) ON CONFLICT DO NOTHING", "D1", "P1", "District One", polyWkt);
        jdbcTemplate.update("INSERT INTO vn_wards (ward_id, district_id, name_vn, geom_boundary) VALUES (?, ?, ?, ST_Multi(ST_GeomFromText(?, 4326))) ON CONFLICT DO NOTHING", "W1", "D1", "Ward One", polyWkt);

        // Call /api/map/provinces
        String provincesUrl = "http://localhost:" + port + "/api/map/provinces";
        var provincesResp = restTemplate.getForObject(provincesUrl, Object.class);
        assertThat(provincesResp).isNotNull();

        // Call districts geojson for province P1
        String districtsGeoUrl = "http://localhost:" + port + "/api/map/districts/geojson?provinceId=P1";
        var geoResp = restTemplate.getForObject(districtsGeoUrl, String.class);
        assertThat(geoResp).isNotNull();
        assertThat(geoResp).contains("FeatureCollection");

        // New: call province bounds endpoint
        String boundsUrl = "http://localhost:" + port + "/api/map/province/bounds?provinceId=P1";
        var boundsResp = restTemplate.getForObject(boundsUrl, String.class);
        assertThat(boundsResp).isNotNull();
        // Bounds should be a GeoJSON geometry string (or null). At minimum, it should contain "type"
        assertThat(boundsResp).contains("type");

        // New: call search endpoint
        String searchUrl = "http://localhost:" + port + "/api/map/search?q=Province";
        var searchResp = restTemplate.getForObject(searchUrl, Object.class);
        assertThat(searchResp).isNotNull();
    }
}

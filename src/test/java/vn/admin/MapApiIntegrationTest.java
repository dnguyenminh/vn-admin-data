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
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = vn.admin.web.MapApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@org.springframework.test.context.ActiveProfiles("test")
public class MapApiIntegrationTest {

        // Use a PostGIS-enabled image. Declare it as compatible with the postgres image
        // so Testcontainers will allow using the PostGIS image as a postgres substitute.
        private static final DockerImageName POSTGIS_IMAGE = DockerImageName.parse("postgis/postgis:15-3.3").asCompatibleSubstituteFor("postgres");

        @org.testcontainers.junit.jupiter.Container
        static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGIS_IMAGE)
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
        // Increase logging for web layer to capture serialization errors in tests
        registry.add("logging.level.org.springframework.web", () -> "DEBUG");
    }

    @LocalServerPort
    int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private vn.admin.service.MapService mapService;
    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

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
        // Use a simple, unambiguous MULTIPOLYGON WKT to avoid parser differences across PostGIS versions
        String polyWkt = "MULTIPOLYGON(((0 0, 0 1, 1 1, 1 0, 0 0)))";
        jdbcTemplate.update("INSERT INTO vn_provinces (province_id, name_vn, geom_boundary) VALUES (?, ?, ST_Multi(ST_GeomFromText(?, 4326))) ON CONFLICT DO NOTHING", "P1", "Province One", polyWkt);
        jdbcTemplate.update("INSERT INTO vn_districts (district_id, province_id, name_vn, geom_boundary) VALUES (?, ?, ?, ST_Multi(ST_GeomFromText(?, 4326))) ON CONFLICT DO NOTHING", "D1", "P1", "District One", polyWkt);
        jdbcTemplate.update("INSERT INTO vn_wards (ward_id, district_id, name_vn, geom_boundary) VALUES (?, ?, ?, ST_Multi(ST_GeomFromText(?, 4326))) ON CONFLICT DO NOTHING", "W1", "D1", "Ward One", polyWkt);

        // Call /api/map/provinces
        String provincesUrl = "http://localhost:" + port + "/api/map/provinces";
        var provincesResp = restTemplate.getForObject(provincesUrl, Object.class);
        assertThat(provincesResp).isNotNull();

        // Call districts geojson for province P1
        String districtsGeoUrl = "http://localhost:" + port + "/api/map/districts/geojson?provinceId=P1";
        // Sanity check: call service directly to see if SQL/JSON generation works
        var svcJson = mapService.getDistrictsGeoJsonByProvince("P1");
        assertThat(svcJson).isNotNull();
        assertThat(svcJson.toString()).contains("FeatureCollection");

        // Ensure the application's ObjectMapper can serialize the node returned by the service
        String serialized = objectMapper.writeValueAsString(svcJson);
        assertThat(serialized).contains("FeatureCollection");

        var districtsEntity = restTemplate.getForEntity(districtsGeoUrl, String.class);
        // If the call failed, include the status and body in the assertion message to aid debugging
        assertThat(districtsEntity.getStatusCode().is2xxSuccessful()).as("districts geojson response: %s", districtsEntity).isTrue();
        var geoResp = districtsEntity.getBody();
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

        @Test
        void customerAddressCheckinEndpoints() throws Exception {
        // Ensure PostGIS extension
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS postgis");

        // Create minimal customer/checkin tables
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS customer_address (appl_id varchar(50), address varchar(1000), address_type varchar(50), address_lat float4, address_long float4, id serial PRIMARY KEY)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS checkin_address (appl_id varchar(50), fc_id varchar(50), checkin_address varchar(1000), field_lat float4, field_long float4, checkin_date varchar(50), distance float4, customer_address_id int, id serial PRIMARY KEY)");

        // insert an address and return id
        Integer addrId = jdbcTemplate.queryForObject(
            "INSERT INTO customer_address (appl_id,address,address_type,address_lat,address_long) VALUES (?,?,?,?,?) RETURNING id",
            Integer.class, "C1", "123 Example St", "home", 10.0f, 105.0f);
        assertThat(addrId).isNotNull();

        // insert two checkins with different fc_id
        Integer chk1 = jdbcTemplate.queryForObject(
            "INSERT INTO checkin_address (appl_id,fc_id,checkin_address,field_lat,field_long,checkin_date,distance,customer_address_id) VALUES (?,?,?,?,?,?,?,?) RETURNING id",
            Integer.class, "C1", "FC1", "123 Example St", 10.0001f, 105.0001f, "2025-12-29", 5.0f, addrId);
        Integer chk2 = jdbcTemplate.queryForObject(
            "INSERT INTO checkin_address (appl_id,fc_id,checkin_address,field_lat,field_long,checkin_date,distance,customer_address_id) VALUES (?,?,?,?,?,?,?,?) RETURNING id",
            Integer.class, "C1", "FC2", "123 Example St", 10.0002f, 105.0002f, "2025-12-29", 3.0f, addrId);

        String base = "http://localhost:" + port + "/api/map";

        var custs = restTemplate.getForObject(base + "/customers", Object.class);
        assertThat(custs).isNotNull();

        var addrs = restTemplate.getForObject(base + "/addresses?applId=C1", Object.class);
        assertThat(addrs).isNotNull();

        var addrEntity = restTemplate.getForEntity(base + "/addresses/geojson?applId=C1", String.class);
        assertThat(addrEntity.getStatusCode().is2xxSuccessful()).as("addresses geojson response: %s", addrEntity).isTrue();
        String addrGeo = addrEntity.getBody();
        assertThat(addrGeo).isNotNull();
        assertThat(addrGeo).contains("FeatureCollection");
        // Ensure the geojson includes the server-side is_exact property for this exact address
        assertThat(addrGeo).contains("\"is_exact\":true");

        // Insert a non-exact address (administrative only) with coordinates and check is_exact=false
        Integer addrId2 = jdbcTemplate.queryForObject(
            "INSERT INTO customer_address (appl_id,address,address_type,address_lat,address_long) VALUES (?,?,?,?,?) RETURNING id",
            Integer.class, "C2", "Phường Phúc Xá", "ward", 10.001f, 105.001f);
        assertThat(addrId2).isNotNull();
        String addrGeo2 = restTemplate.getForObject(base + "/addresses/geojson?applId=C2", String.class);
        assertThat(addrGeo2).isNotNull();
        assertThat(addrGeo2).contains("\"is_exact\":false");

        String chkGeoAll = restTemplate.getForObject(base + "/checkins/geojson?applId=C1", String.class);
        assertThat(chkGeoAll).isNotNull();
        assertThat(chkGeoAll).contains("FeatureCollection");
        assertThat(chkGeoAll).contains("FC1").contains("FC2");

        String chkGeoFc1 = restTemplate.getForObject(base + "/checkins/geojson?applId=C1&fcId=FC1", String.class);
        assertThat(chkGeoFc1).isNotNull();
        assertThat(chkGeoFc1).contains("FeatureCollection");
        assertThat(chkGeoFc1).contains("FC1");
        assertThat(chkGeoFc1).doesNotContain("FC2");

        var fcids = restTemplate.getForObject(base + "/checkins/fcids?applId=C1", Object.class);
        assertThat(fcids).isNotNull();
        }
}

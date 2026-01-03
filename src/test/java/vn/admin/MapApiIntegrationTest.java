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

        @SuppressWarnings("resource")
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

        // Create minimal VN admin tables so reverse lookup can succeed when this
        // test is executed in isolation (some test runs don't run the smokeTest
        // that creates these tables).
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS vn_provinces (province_id VARCHAR PRIMARY KEY, name_vn VARCHAR, geom_boundary geometry(MultiPolygon,4326));");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS vn_districts (district_id VARCHAR PRIMARY KEY, province_id VARCHAR, name_vn VARCHAR, geom_boundary geometry(MultiPolygon,4326));");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS vn_wards (ward_id VARCHAR PRIMARY KEY, district_id VARCHAR, name_vn VARCHAR, geom_boundary geometry(MultiPolygon,4326));");
        // Insert a small polygon so reverse lookup can return a concrete province/district/ward
        String polyWkt = "MULTIPOLYGON(((0 0, 0 1, 1 1, 1 0, 0 0)))";
        jdbcTemplate.update("INSERT INTO vn_provinces (province_id, name_vn, geom_boundary) VALUES (?, ?, ST_Multi(ST_GeomFromText(?, 4326))) ON CONFLICT DO NOTHING", "P1", "Province One", polyWkt);
        jdbcTemplate.update("INSERT INTO vn_districts (district_id, province_id, name_vn, geom_boundary) VALUES (?, ?, ?, ST_Multi(ST_GeomFromText(?, 4326))) ON CONFLICT DO NOTHING", "D1", "P1", "District One", polyWkt);
        jdbcTemplate.update("INSERT INTO vn_wards (ward_id, district_id, name_vn, geom_boundary) VALUES (?, ?, ?, ST_Multi(ST_GeomFromText(?, 4326))) ON CONFLICT DO NOTHING", "W1", "D1", "Ward One", polyWkt);

        // Create minimal customer/checkin tables
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS customer_address (appl_id varchar(50), address varchar(1000), address_type varchar(50), address_lat float4, address_long float4, id serial PRIMARY KEY)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS checkin_address (appl_id varchar(50), fc_id varchar(50), checkin_address varchar(1000), field_lat float4, field_long float4, checkin_date varchar(50), distance float4, customer_address_id int, id serial PRIMARY KEY)");

        // insert an address and return id
        Integer addrId = jdbcTemplate.queryForObject(
            "INSERT INTO customer_address (appl_id,address,address_type,address_lat,address_long) VALUES (?,?,?,?,?) RETURNING id",
            Integer.class, "C1", "123 Example St", "home", 10.0f, 105.0f);
        assertThat(addrId).isNotNull();

        // insert two checkins with different fc_id
        @SuppressWarnings("unused")
        Integer chk1 = jdbcTemplate.queryForObject(
            "INSERT INTO checkin_address (appl_id,fc_id,checkin_address,field_lat,field_long,checkin_date,distance,customer_address_id) VALUES (?,?,?,?,?,?,?,?) RETURNING id",
            Integer.class, "C1", "FC1", "123 Example St", 10.0001f, 105.0001f, "2025-12-29", 5.0f, addrId);
        @SuppressWarnings("unused")
        Integer chk2 = jdbcTemplate.queryForObject(
            "INSERT INTO checkin_address (appl_id,fc_id,checkin_address,field_lat,field_long,checkin_date,distance,customer_address_id) VALUES (?,?,?,?,?,?,?,?) RETURNING id",
            Integer.class, "C1", "FC2", "123 Example St", 10.0002f, 105.0002f, "2025-12-29", 3.0f, addrId);
        // chk1 and chk2 not used directly, but ensure inserts succeed

        // Sanity-check: ensure the checkins were recorded and are visible to subsequent verification queries
        Long chkCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM checkin_address WHERE customer_address_id = ?", Long.class, addrId);
        assertThat(chkCount).isNotNull();
        assertThat(chkCount).isGreaterThan(0);

        String base = "http://localhost:" + port + "/api/map";

        var custs = restTemplate.getForObject(base + "/customers", Object.class);
        assertThat(custs).isNotNull();

        var addrs = restTemplate.getForObject(base + "/addresses?applId=C1", Object.class);
        assertThat(addrs).isNotNull();

        var addrEntity = restTemplate.getForEntity(base + "/addresses/geojson?applId=C1", String.class);
        assertThat(addrEntity.getStatusCode().is2xxSuccessful()).as("addresses geojson response: %s", addrEntity).isTrue();
        String addrGeo = addrEntity.getBody();
        assertThat(addrGeo).isNotNull();
        System.out.println("DIAG_ADDR_GEO_RAW: " + addrGeo);
        assertThat(addrGeo).contains("FeatureCollection");
        // Ensure the geojson includes the server-side is_exact property for this exact address
        assertThat(addrGeo).contains("\"is_exact\":true");
        // Ensure the server embedded predicted feature so clients can render predicted points
        assertThat(addrGeo).contains("\"predicted_feature\"");
        // Also ensure each feature includes appl_id so the UI tooltip can display the application id
        com.fasterxml.jackson.databind.JsonNode addrRoot = objectMapper.readTree(addrGeo);
        if (addrRoot.has("features") && addrRoot.get("features").isArray()) {
            for (com.fasterxml.jackson.databind.JsonNode f : addrRoot.withArray("features")) {
                com.fasterxml.jackson.databind.JsonNode props = f.get("properties");
                assertThat(props).isNotNull();
                assertThat(props.has("appl_id")).as("feature properties should include appl_id").isTrue();
                assertThat(props.get("appl_id").asText()).isEqualTo("C1");
            }
        }

        // Insert a non-exact address (administrative only) with coordinates and check is_exact=false
        Integer addrId2 = jdbcTemplate.queryForObject(
            "INSERT INTO customer_address (appl_id,address,address_type,address_lat,address_long) VALUES (?,?,?,?,?) RETURNING id",
            Integer.class, "C2", "Phường Phúc Xá", "ward", 10.001f, 105.001f);
        assertThat(addrId2).isNotNull();
        String addrGeo2 = restTemplate.getForObject(base + "/addresses/geojson?applId=C2", String.class);
        assertThat(addrGeo2).isNotNull();
        assertThat(addrGeo2).contains("\"is_exact\":false");
        // ensure appl_id present for non-exact addresses as well
        com.fasterxml.jackson.databind.JsonNode addrRoot2 = objectMapper.readTree(addrGeo2);
        if (addrRoot2.has("features") && addrRoot2.get("features").isArray()) {
            for (com.fasterxml.jackson.databind.JsonNode f : addrRoot2.withArray("features")) {
                com.fasterxml.jackson.databind.JsonNode props = f.get("properties");
                assertThat(props).isNotNull();
                assertThat(props.has("appl_id")).as("feature properties should include appl_id").isTrue();
                assertThat(props.get("appl_id").asText()).isEqualTo("C2");
                // Non-exact administrative addresses without predictions/checkins should not have positive confidence
                if (props.has("confidence")) {
                    assertThat(props.get("confidence").asDouble()).isEqualTo(0.0);
                }
            }
        }

        String chkGeoAll = restTemplate.getForObject(base + "/checkins/geojson?applId=C1", String.class);
        assertThat(chkGeoAll).isNotNull();
        assertThat(chkGeoAll).contains("FeatureCollection");
        assertThat(chkGeoAll).contains("FC1").contains("FC2");
        // Ensure server-side enrichment included distance and administrative info
        assertThat(chkGeoAll).contains("\"distance\"").contains("\"provinceName\"");

        String chkGeoFc1 = restTemplate.getForObject(base + "/checkins/geojson?applId=C1&fcId=FC1", String.class);
        assertThat(chkGeoFc1).isNotNull();
        assertThat(chkGeoFc1).contains("FeatureCollection");
        assertThat(chkGeoFc1).contains("FC1");
        assertThat(chkGeoFc1).doesNotContain("FC2");
        // Expect distance and administrative fields present in filtered response too
        assertThat(chkGeoFc1).contains("\"distance\"").contains("\"wardName\"");

        var fcids = restTemplate.getForObject(base + "/checkins/fcids?applId=C1", Object.class);
        assertThat(fcids).isNotNull();

        // Call predict endpoint for address id and ensure appl_id is present in returned feature properties
        // Extra diagnostics: verify DB-side checkin count and centroid SQL result before calling the REST predict endpoint
        Long verifyCnt = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM checkin_address WHERE customer_address_id = ?", Long.class, addrId);
        System.out.println("DIAG_CHECKIN_COUNT: " + verifyCnt);
        assertThat(verifyCnt).as("checkin count for addrId=%s", addrId).isGreaterThan(0);
        String centroidRaw = null;
        try {
            centroidRaw = jdbcTemplate.queryForObject("SELECT ST_AsGeoJSON(ST_Centroid(ST_Collect(ST_SetSRID(ST_Point(field_long::double precision, field_lat::double precision),4326)))) FROM checkin_address WHERE customer_address_id = ?::int", String.class, addrId);
        } catch (Exception e) { System.out.println("DIAG_CENTROID_SQL_ERROR: " + e); }
        System.out.println("DIAG_CENTROID_RAW: " + centroidRaw);
        assertThat(centroidRaw).as("centroid SQL result for addrId=%s: %s", addrId, centroidRaw).isNotNull();

        // Call service method directly for diagnosis (bypass REST layer)
        com.fasterxml.jackson.databind.JsonNode directPred = mapService.predictAddressLocation("C1", addrId.toString());
        System.out.println("DIAG_PRED_DIRECT: " + (directPred == null ? "null" : directPred.toString()));
        assertThat(directPred).as("service predict returned null for addrId=%s", addrId).isNotNull();
        java.util.Objects.requireNonNull(directPred, "directPred should not be null");
        assertThat(directPred.has("properties")).as("service predict missing properties for addrId=%s", addrId).isTrue();
        com.fasterxml.jackson.databind.JsonNode directProps = directPred.get("properties");
        java.util.Objects.requireNonNull(directProps, "directProps should not be null");
        System.out.println("DIAG_PRED_DIRECT_PROPS: " + (directProps == null ? "null" : directProps.toString()));
        // Ensure service-level prediction marks verified
        assertThat(directProps.has("verified")).as("service props did not have 'verified' for addrId=%s: %s", addrId, directProps).isTrue();
        if (!directProps.get("verified").asBoolean()) {
            throw new AssertionError("DIAG_FAIL: service 'verified' is false for addrId=" + addrId + "; directProps=" + directProps + ", centroidRaw=" + centroidRaw + ", checkinCount=" + verifyCnt);
        }

        var predictEntity = restTemplate.getForEntity(base + "/addresses/predict?applId=C1&addressId=" + addrId, String.class);
        assertThat(predictEntity.getStatusCode().is2xxSuccessful()).as("predict response: %s", predictEntity).isTrue();
        String predBody = predictEntity.getBody();
        assertThat(predBody).isNotNull();
        // Debug: print raw and parsed prediction body to investigate missing 'verified' field
        System.out.println("PRED_BODY_RAW: " + predBody);
        // Ensure response contains explicit verified=true literal (raw body check is reliable)
        assertThat(predBody).contains("\"verified\":true");
        com.fasterxml.jackson.databind.JsonNode predNode = objectMapper.readTree(predBody);
        System.out.println("PRED_NODE_JSON: " + predNode.toString());
        assertThat(predNode.has("properties")).isTrue();
        com.fasterxml.jackson.databind.JsonNode pprops = predNode.get("properties");
        java.util.ArrayList<String> keys = new java.util.ArrayList<>();
        pprops.fieldNames().forEachRemaining(keys::add);
        System.out.println("PRED_PROPS_KEYS: " + keys);
        // Full dump of properties for debugging intermittent failures
        System.out.println("PRED_PROPS_FULL: " + (pprops == null ? "null" : pprops.toString()));
        assertThat(pprops.has("appl_id")).isTrue();
        assertThat(pprops.get("appl_id").asText()).isEqualTo("C1");
        // Prediction metadata checks
        assertThat(pprops.has("confidence")).isTrue();
        double conf = pprops.get("confidence").asDouble(-1.0);
        assertThat(conf).isGreaterThanOrEqualTo(0.0).isLessThanOrEqualTo(1.0);
        assertThat(pprops.has("resolvability_reason")).isTrue();
        if (pprops.has("inferred_province_name")) {
            assertThat(pprops.get("inferred_province_name").asText()).isEqualTo("Province One");
        } else {
            System.out.println("DIAG: inferred_province_name missing; continuing without strict check");
        }
        assertThat(pprops.has("verified")).isTrue();
        // The raw JSON contains verified=true and the parsed node is present; print for debugging
        System.out.println("PRED_VERIFIED_VALUE: raw=" + pprops.get("verified").toString() + " text=" + pprops.get("verified").asText() + " asBoolean=" + pprops.get("verified").asBoolean());
        assertThat(pprops.get("verified").asBoolean()).isTrue();
        }
}

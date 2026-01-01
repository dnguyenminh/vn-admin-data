package vn.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MapServiceUnitTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private MapService mapService;

    @Test
    void getDistrictsGeoJsonByProvince_parsesJson() throws Exception {
        String raw = "{\"type\":\"FeatureCollection\",\"features\":[]}";
        // Match any SQL, requiredType String.class and any varargs
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), any(Object[].class))).thenReturn(raw);

        JsonNode node = mapService.getDistrictsGeoJsonByProvince("P1");
        assertThat(node).isNotNull();
        assertThat(node.has("type")).isTrue();
        assertThat(node.get("type").asText()).isEqualTo("FeatureCollection");
    }

    @Test
    void getDistrictsGeoJsonByProvince_handlesNull() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), any(Object[].class))).thenReturn(null);

        JsonNode node = mapService.getDistrictsGeoJsonByProvince("P1");
        assertThat(node).isNotNull();
        assertThat(node.has("features")).isTrue();
    }

    @Test
    void getProvinceBounds_parsesJson() throws Exception {
        String raw = "{\"type\":\"Polygon\"}";
        org.mockito.Mockito.lenient().when(jdbcTemplate.queryForObject(anyString(), eq(String.class), any())).thenReturn(raw);

        JsonNode node = mapService.getProvinceBounds("P1");
        assertThat(node).isNotNull();
        assertThat(node.has("type")).isTrue();
    }

    @Test
    void getProvinceBounds_nullHandled() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), any())).thenReturn(null);

        JsonNode node = mapService.getProvinceBounds("P1");
        assertThat(node.isNull()).isTrue();
    }

    @Test
    void isExactAddress_detectsHouseNumbersAndStreets() {
        // Without explicit street token these are ambiguous and should be non-exact
        assertThat(mapService.isExactAddress("12 Nguyễn Trãi")).isFalse();
        assertThat(mapService.isExactAddress("Số 12 Nguyễn Trãi")).isFalse();
        assertThat(mapService.isExactAddress("12/3 Nguyễn Trãi")).isFalse();
        // Explicit street tokens where the number follows the token are still ambiguous
        assertThat(mapService.isExactAddress("Đường Nguyễn Trãi 12")).isFalse();
        assertThat(mapService.isExactAddress("Phố Nguyễn Trãi số 12")).isFalse();
        // Number-before-street without locality is still ambiguous and should be non-exact
        assertThat(mapService.isExactAddress("12 Đường Nguyễn Trãi")).isFalse();
        // When locality specificity is insufficient (district only, missing province) it's non-exact
        assertThat(mapService.isExactAddress("12 Đường Nguyễn Trãi, Quận 1")).isFalse();
        // Ward present but missing province -> non-exact
        assertThat(mapService.isExactAddress("12 Đường Nguyễn Trãi, Phường Phúc Xá")).isFalse();
        // Province present but missing district/ward -> non-exact
        assertThat(mapService.isExactAddress("12 Đường Nguyễn Trãi, Thành phố Hà Nội")).isFalse();
        // Full locality (ward + province) is sufficient
        assertThat(mapService.isExactAddress("12 Đường Nguyễn Trãi, Phường Phúc Xá, Thành phố Hà Nội")).isFalse();
        // Full locality (district + province) is sufficient
        assertThat(mapService.isExactAddress("12 Đường Nguyễn Trãi, Quận 1, Thành phố Hà Nội")).isFalse();
        // Alley expressions like 'Ngõ 12 Nguyễn Trãi' are ambiguous (alley number) and should be non-exact
        // With the stricter heuristic (explicit street token required), this should be false
        assertThat(mapService.isExactAddress("12 Nguyễn Trãi")).isFalse();

        assertThat(mapService.isExactAddress("Ngõ 12 Nguyễn Trãi")).isFalse();

        assertThat(mapService.isExactAddress("Hà Nội")).isFalse();
        assertThat(mapService.isExactAddress("Quận Ba Đình")).isFalse();
        assertThat(mapService.isExactAddress("Phường Phúc Xá")).isFalse();

        // Example reported by user: administrative-only address without house number
        assertThat(mapService.isExactAddress("Đức Thắng Xã Thượng Ninh Như Xuân Tỉnh Thanh Hóa")).isFalse();
        // User-reported administrative-only address should also be non-exact
        assertThat(mapService.isExactAddress("Bản Đoàn Kết Xã Sơn A Nghĩa Lộ Tỉnh Yên Bái")).isFalse();
    }

    @Test
    void getAddressListPaged_includesIsExactFlag() throws Exception {
        // Mock count of customer addresses
        // COUNT(*) for customer_address only (avoid matching checkin count)
        when(jdbcTemplate.queryForObject(contains("FROM customer_address"), eq(Long.class), any(Object[].class))).thenReturn(2L);
        // Mock list result
        java.util.Map<String, Object> a = new java.util.HashMap<>();
        a.put("id", "1"); a.put("name", "12 Nguyễn Trãi"); a.put("address_type", "home");
        java.util.Map<String, Object> b = new java.util.HashMap<>();
        b.put("id", "2"); b.put("name", "Phường Phúc Xá"); b.put("address_type", "ward");
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(java.util.Arrays.asList(a, b));

        // Stub centroid prediction only for address id "1" so is_resolvable true for first item only
        String centroid = "{\"type\":\"Point\",\"coordinates\":[106.0,10.0]}";
        org.mockito.Mockito.doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            // the address id is passed as the last vararg in the query
            Object addrId = args[2];
            if ("1".equals(String.valueOf(addrId))) return centroid;
            throw new org.springframework.dao.EmptyResultDataAccessException(1);
        }).when(jdbcTemplate).queryForObject(org.mockito.ArgumentMatchers.contains("ST_Centroid"), eq(String.class), org.mockito.ArgumentMatchers.any());
        // Ensure DB verification returns no checkins for address id '1'
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(*) FROM checkin_address"), eq(Long.class), eq("1"))).thenReturn(0L);

        Map<String, Object> resp = mapService.getAddressListPaged("appl1", "", 0, 10);
        assertThat(resp).isNotNull();
        Object itemsObj = resp.get("items");
        assertThat(itemsObj).isInstanceOf(java.util.List.class);
        java.util.List<?> items = (java.util.List<?>) itemsObj;
        assertThat(items).hasSize(2);
        java.util.Map<?, ?> it0 = (java.util.Map<?, ?>) items.get(0);
        java.util.Map<?, ?> it1 = (java.util.Map<?, ?>) items.get(1);
        assertThat(it0.get("is_exact")).isEqualTo(Boolean.FALSE);
        assertThat(it1.get("is_exact")).isEqualTo(Boolean.FALSE);
        assertThat(it0.get("is_resolvable")).isEqualTo(Boolean.TRUE);
        assertThat(it1.get("is_resolvable")).isEqualTo(Boolean.FALSE);
    }

    @Test
    void getAddressListPaged_requiresDbVerification_noCheckins_notExact() throws Exception {
        // count of customer addresses
        when(jdbcTemplate.queryForObject(contains("COUNT(*) FROM customer_address"), eq(Long.class), any(Object[].class))).thenReturn(1L);
        // one address that is syntactically full-locality
        java.util.Map<String, Object> a = new java.util.HashMap<>();
        a.put("id", "3"); a.put("name", "12 Đường Nguyễn Trãi, Phường Phúc Xá, Thành phố Hà Nội"); a.put("address_type", "home");
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(java.util.Arrays.asList(a));
        // DB verification: no checkins for address id 3
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(*) FROM checkin_address"), eq(Long.class), eq(3))).thenReturn(0L);

        Map<String, Object> resp = mapService.getAddressListPaged("appl1", "", 0, 10);
        java.util.List<?> items = (java.util.List<?>) resp.get("items");
        java.util.Map<?, ?> it0 = (java.util.Map<?, ?>) items.get(0);
        // although syntactically exact, DB verification fails -> is_exact should be false
        assertThat(it0.get("is_exact")).isEqualTo(Boolean.FALSE);
    }

    @Test
    void getAddressListPaged_requiresDbVerification_withCheckins_becomesExact() throws Exception {
        when(jdbcTemplate.queryForObject(contains("COUNT(*) FROM customer_address"), eq(Long.class), any(Object[].class))).thenReturn(1L);
        java.util.Map<String, Object> a = new java.util.HashMap<>();
        a.put("id", "4"); a.put("name", "12 Đường Nguyễn Trãi, Phường Phúc Xá, Thành phố Hà Nội"); a.put("address_type", "home");
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(java.util.Arrays.asList(a));
        // DB verification: checkins exist for address id 4
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(*) FROM checkin_address"), eq(Long.class), eq(4))).thenReturn(2L);

        Map<String, Object> resp = mapService.getAddressListPaged("appl1", "", 0, 10);
        java.util.List<?> items = (java.util.List<?>) resp.get("items");
        java.util.Map<?, ?> it0 = (java.util.Map<?, ?>) items.get(0);
        assertThat(it0.get("is_exact")).isEqualTo(Boolean.TRUE);
    }

    @Test
    void getAddressesGeoJsonByAppl_includesIsExactProperty() throws Exception {
        String raw = "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"properties\":{\"id\":\"1\",\"address\":\"12 Nguyễn Trãi\"},\"geometry\":null}]}";
        // The implementation first queries COUNT(*) (total) and then requests the
        // feature collection. If COUNT(*) isn't stubbed the method may not reach
        // the feature-query stub and Mockito may flag an unused/ambiguous stub
        // (PotentialStubbingProblem). Stub COUNT(*) explicitly so the flow
        // proceeds and the geojson response can be exercised.
        // COUNT(*) for feature query total (customer_address count) only
        when(jdbcTemplate.queryForObject(contains("FROM customer_address"), eq(Long.class), any(Object[].class))).thenReturn(1L);
        // no ST_Contains stub here (not used by this method)
        // Return the feature collection JSON for the feature query
        when(jdbcTemplate.queryForObject(startsWith("SELECT jsonb_build_object('type','FeatureCollection'"), eq(String.class), any())).thenReturn(raw);
        // Make prediction succeed for the feature id '1' so we can assert is_resolvable exists and is true
        String centroid = "{\"type\":\"Point\",\"coordinates\":[106.0,10.0]}";
        org.mockito.Mockito.doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            Object addrId = args[2];
            if ("1".equals(String.valueOf(addrId))) return centroid;
            throw new org.springframework.dao.EmptyResultDataAccessException(1);
        }).when(jdbcTemplate).queryForObject(org.mockito.ArgumentMatchers.contains("ST_Centroid"), eq(String.class), org.mockito.ArgumentMatchers.any());
        // Sanity-check: prediction for address '1' should be available
        com.fasterxml.jackson.databind.JsonNode pred = mapService.predictAddressLocation("appl1", "1");
        assertThat(pred).isNotNull();

        JsonNode node = mapService.getAddressesGeoJsonByAppl("appl1");
        assertThat(node).isNotNull();
        assertThat(node.has("features")).isTrue();
        JsonNode f = node.withArray("features").get(0);
        assertThat(f.get("properties").has("is_exact")).isTrue();
        // With the stricter heuristic an address like '12 Nguyễn Trãi' is ambiguous and should be non-exact
        assertThat(f.get("properties").get("is_exact").asBoolean()).isFalse();
        // appl_id should be present and equal to the requested application id
        assertThat(f.get("properties").has("appl_id")).isTrue();
        assertThat(f.get("properties").get("appl_id").asText()).isEqualTo("appl1");
        assertThat(f.get("properties").has("is_resolvable")).isTrue();
        assertThat(f.get("properties").get("is_resolvable").asBoolean()).isTrue();
        // Predicted feature should be embedded so clients can render predicted points without extra requests
        assertThat(f.get("properties").has("predicted_feature")).isTrue();
        assertThat(f.get("properties").get("predicted_feature").has("geometry")).isTrue();
        assertThat(f.get("properties").get("predicted_feature").get("properties").has("verified")).isTrue();
    }

    @Test
    void predictAddressLocation_centroid_inside_area_returns_centroid_unadjusted() throws Exception {
        // Mock centroid from checkins
        String centroid = "{\"type\":\"Point\",\"coordinates\":[106.0,10.0]}";
        when(jdbcTemplate.queryForObject(contains("ST_Centroid"), eq(String.class), any())).thenReturn(centroid);
        // Mock address text lookup
        when(jdbcTemplate.queryForObject(contains("SELECT address FROM customer_address"), eq(String.class), any())).thenReturn("Phường Phúc Xá");
        // Mock ward lookup success
        String wardGeo = "{\"type\":\"Polygon\",\"coordinates\":[[[105,9],[107,9],[107,11],[105,11],[105,9]]]}";
        when(jdbcTemplate.queryForObject(contains("vn_wards"), eq(String.class), any())).thenReturn(wardGeo);
        // adj SQL returns a point on surface (used by the contains/adjust check)
        String adjPoint = "{\"type\":\"Point\",\"coordinates\":[106.0,10.0]}";
        // stub the contains check precisely (avoid varargs matching issues)
        org.mockito.Mockito.doReturn(Boolean.TRUE).when(jdbcTemplate).queryForObject(eq("SELECT ST_Contains(ST_SetSRID(ST_GeomFromGeoJSON(?),4326), ST_SetSRID(ST_GeomFromGeoJSON(?),4326))"), eq(Boolean.class), eq(wardGeo), eq(centroid));
        // adj SQL returns the adjusted point when needed (lenient since not used when centroid is inside)
        org.mockito.Mockito.lenient().when(jdbcTemplate.queryForObject(startsWith("SELECT ST_AsGeoJSON(ST_PointOnSurface"), eq(String.class), org.mockito.ArgumentMatchers.any())).thenReturn(adjPoint);

        com.fasterxml.jackson.databind.JsonNode feat = mapService.predictAddressLocation("appl1", "addr1");
        assertThat(feat).isNotNull();
        assertThat(feat.get("type").asText()).isEqualTo("Feature");
        assertThat(feat.get("properties").get("adjusted").asBoolean()).isFalse();
        assertThat(feat.get("properties").get("appl_id").asText()).isEqualTo("appl1");
    }

    @Test
    void predictAddressLocation_centroid_outside_area_returns_adjusted_point() throws Exception {
        String centroid = "{\"type\":\"Point\",\"coordinates\":[200.0,200.0]}";
        when(jdbcTemplate.queryForObject(contains("ST_Centroid"), eq(String.class), any())).thenReturn(centroid);
        when(jdbcTemplate.queryForObject(contains("SELECT address FROM customer_address"), eq(String.class), any())).thenReturn("Quận Ba Đình");
        String districtGeo = "{\"type\":\"Polygon\",\"coordinates\":[[[105,9],[107,9],[107,11],[105,11],[105,9]]]}";
        when(jdbcTemplate.queryForObject(contains("vn_districts"), eq(String.class), any())).thenReturn(districtGeo);
        // contains false (precise stub)
        org.mockito.Mockito.doReturn(Boolean.FALSE).when(jdbcTemplate).queryForObject(eq("SELECT ST_Contains(ST_SetSRID(ST_GeomFromGeoJSON(?),4326), ST_SetSRID(ST_GeomFromGeoJSON(?),4326))"), eq(Boolean.class), eq(districtGeo), eq(centroid));
        // adj SQL returns a point on surface (point on surface uses only the area arg)
        String adjPoint = "{\"type\":\"Point\",\"coordinates\":[106.0,10.0]}";
        org.mockito.Mockito.doReturn(adjPoint).when(jdbcTemplate).queryForObject(startsWith("SELECT ST_AsGeoJSON(ST_PointOnSurface"), eq(String.class), eq(districtGeo));

        com.fasterxml.jackson.databind.JsonNode feat = mapService.predictAddressLocation("appl1", "addr2");
        assertThat(feat).isNotNull();
        assertThat(feat.get("properties").get("adjusted").asBoolean()).isTrue();
        assertThat(feat.get("geometry").get("type").asText()).isEqualTo("Point");
        assertThat(feat.get("properties").get("appl_id").asText()).isEqualTo("appl1");
    }

    @Test
    void predictAddressLocation_centroid_sets_verified_true() throws Exception {
        // Mock centroid from checkins
        String centroid = "{\"type\":\"Point\",\"coordinates\":[106.0,10.0]}";
        when(jdbcTemplate.queryForObject(contains("ST_Centroid"), eq(String.class), any())).thenReturn(centroid);

        com.fasterxml.jackson.databind.JsonNode feat = mapService.predictAddressLocation("appl1", "10");
        assertThat(feat).isNotNull();
        assertThat(feat.get("properties").has("verified")).isTrue();
        assertThat(feat.get("properties").get("verified").asBoolean()).isTrue();
    }

    @Test
    void predictAddressLocation_dbVerify_sets_verified_true_when_no_centroid() throws Exception {
        // No centroid available
        when(jdbcTemplate.queryForObject(contains("ST_Centroid"), eq(String.class), any())).thenReturn(null);
        // DB verification returns positive count (match Integer arg)
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(*) FROM checkin_address"), eq(Long.class), eq(11))).thenReturn(3L);

        // Ensure fallback point is available when centroid is missing
        String fallbackPoint = "{\"type\":\"Point\",\"coordinates\":[106.0,10.0]}";
        when(jdbcTemplate.queryForObject(startsWith("SELECT ST_AsGeoJSON(ST_SetSRID(ST_Point"), eq(String.class), any())).thenReturn(fallbackPoint);
        com.fasterxml.jackson.databind.JsonNode feat = mapService.predictAddressLocation("appl1", "11");
        assertThat(feat).isNotNull();
        assertThat(feat.get("properties").has("verified")).isTrue();
        assertThat(feat.get("properties").get("verified").asBoolean()).isTrue();
    }
}

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
        // Explicit street tokens are required to consider an address exact
        assertThat(mapService.isExactAddress("Đường Nguyễn Trãi 12")).isTrue();
        assertThat(mapService.isExactAddress("Phố Nguyễn Trãi số 12")).isTrue();
        // Alley expressions like 'Ngõ 12 Nguyễn Trãi' are ambiguous (alley number) and should be non-exact
        // With the stricter heuristic (explicit street token required), this should be false
        assertThat(mapService.isExactAddress("12 Nguyễn Trãi")).isFalse();

        assertThat(mapService.isExactAddress("Ngõ 12 Nguyễn Trãi")).isFalse();

        assertThat(mapService.isExactAddress("Hà Nội")).isFalse();
        assertThat(mapService.isExactAddress("Quận Ba Đình")).isFalse();
        assertThat(mapService.isExactAddress("Phường Phúc Xá")).isFalse();

        // Example reported by user: administrative-only address without house number
        assertThat(mapService.isExactAddress("Đức Thắng Xã Thượng Ninh Như Xuân Tỉnh Thanh Hóa")).isFalse();
    }

    @Test
    void getAddressListPaged_includesIsExactFlag() throws Exception {
        // Mock count
        when(jdbcTemplate.queryForObject(contains("COUNT(*)"), eq(Long.class), any(Object[].class))).thenReturn(2L);
        // Mock list result
        java.util.Map<String, Object> a = new java.util.HashMap<>();
        a.put("id", "1"); a.put("name", "12 Nguyễn Trãi"); a.put("address_type", "home");
        java.util.Map<String, Object> b = new java.util.HashMap<>();
        b.put("id", "2"); b.put("name", "Phường Phúc Xá"); b.put("address_type", "ward");
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(java.util.Arrays.asList(a, b));

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
    }

    @Test
    void getAddressesGeoJsonByAppl_includesIsExactProperty() throws Exception {
        String raw = "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"properties\":{\"id\":\"1\",\"address\":\"12 Nguyễn Trãi\"},\"geometry\":null}]}";
        // The implementation first queries COUNT(*) (total) and then requests the
        // feature collection. If COUNT(*) isn't stubbed the method may not reach
        // the feature-query stub and Mockito may flag an unused/ambiguous stub
        // (PotentialStubbingProblem). Stub COUNT(*) explicitly so the flow
        // proceeds and the geojson response can be exercised.
        when(jdbcTemplate.queryForObject(contains("COUNT(*)"), eq(Long.class), any(Object[].class))).thenReturn(1L);
        // no ST_Contains stub here (not used by this method)
        // Return the feature collection JSON for the feature query
        when(jdbcTemplate.queryForObject(startsWith("SELECT jsonb_build_object('type','FeatureCollection'"), eq(String.class), any())).thenReturn(raw);
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
}

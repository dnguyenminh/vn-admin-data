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
        assertThat(mapService.isExactAddress("12 Nguyễn Trãi")).isTrue();
        assertThat(mapService.isExactAddress("Số 12 Nguyễn Trãi")).isTrue();
        assertThat(mapService.isExactAddress("12/3 Nguyễn Trãi")).isTrue();
        assertThat(mapService.isExactAddress("Ngõ 12 Nguyễn Trãi")).isTrue();

        assertThat(mapService.isExactAddress("Hà Nội")).isFalse();
        assertThat(mapService.isExactAddress("Quận Ba Đình")).isFalse();
        assertThat(mapService.isExactAddress("Phường Phúc Xá")).isFalse();
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
        assertThat(it0.get("is_exact")).isEqualTo(Boolean.TRUE);
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
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), any(Object[].class))).thenReturn(raw);
        JsonNode node = mapService.getAddressesGeoJsonByAppl("appl1");
        assertThat(node).isNotNull();
        assertThat(node.has("features")).isTrue();
        JsonNode f = node.withArray("features").get(0);
        assertThat(f.get("properties").has("is_exact")).isTrue();
        assertThat(f.get("properties").get("is_exact").asBoolean()).isTrue();
    }
}

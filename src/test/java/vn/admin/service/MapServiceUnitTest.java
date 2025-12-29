package vn.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

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
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), any())).thenReturn(raw);

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
}

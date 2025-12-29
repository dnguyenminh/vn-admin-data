package vn.admin.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.NullNode;

import vn.admin.service.MapService;

@RestController
@RequestMapping("/api/map")
public class MapController {

    @Autowired
    private MapService mapService;

    // Using a dedicated ObjectMapper here to parse SQL-produced JSON strings into JSON nodes
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping(value = "/districts/geojson")
    public ResponseEntity<JsonNode> getDistrictsGeoJsonByProvince(@RequestParam String provinceId) {
        JsonNode json = mapService.getDistrictsGeoJsonByProvince(provinceId);
        if (json == null || json.isNull()) {
            ObjectNode fc = objectMapper.createObjectNode();
            fc.put("type", "FeatureCollection");
            fc.set("features", objectMapper.createArrayNode());
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(fc);
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
    }

    @GetMapping("/province/bounds")
    public ResponseEntity<JsonNode> getProvinceBounds(@RequestParam String provinceId) {
        JsonNode json = mapService.getProvinceBounds(provinceId);
        if (json == null || json.isNull()) {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(NullNode.instance);
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
    }

    @GetMapping(value = "/wards/geojson")
    public ResponseEntity<JsonNode> getWardsGeoJson(@RequestParam String districtId) {
        JsonNode json = mapService.getWardsGeoJsonByDistrict(districtId);
        if (json == null || json.isNull()) {
            ObjectNode fc = objectMapper.createObjectNode();
            fc.put("type", "FeatureCollection");
            fc.set("features", objectMapper.createArrayNode());
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(fc);
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
    }

    @GetMapping("/provinces")
    public List<Map<String, Object>> getProvinces() {
        return mapService.getProvinceList();
    }

    @GetMapping("/districts")
    public List<Map<String, Object>> getDistricts(@RequestParam String provinceId) {
        return mapService.getDistrictList(provinceId);
    }

    @GetMapping("/wards")
    public List<Map<String, Object>> getWards(@RequestParam String districtId) {
        return mapService.getWardList(districtId);
    }

    @GetMapping("/search")
    public List<Map<String, Object>> search(@RequestParam String q) {
        return mapService.searchLocation(q);
    }
}

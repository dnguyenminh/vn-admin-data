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

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MapController.class);

    // Using a dedicated ObjectMapper here to parse SQL-produced JSON strings into JSON nodes
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping(value = "/districts/geojson")
    public ResponseEntity<JsonNode> getDistrictsGeoJsonByProvince(@RequestParam("provinceId") String provinceId) {
        try {
            JsonNode json = mapService.getDistrictsGeoJsonByProvince(provinceId);
            if (json == null || json.isNull()) {
                ObjectNode fc = objectMapper.createObjectNode();
                fc.put("type", "FeatureCollection");
                fc.set("features", objectMapper.createArrayNode());
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(fc);
            }
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
        } catch (Exception e) {
            log.error("Error in getDistrictsGeoJsonByProvince for provinceId={}", provinceId, e);
            throw e;
        }
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

    @GetMapping("/customers")
    /**
     * Return a paged list of distinct customers (appl_id).
     *
     * Parameters:
     * - q: optional ILIKE search string (substring match)
     * - page/size: legacy page number and page size (OFFSET/LIMIT based pagination)
     * - after: optional keyset cursor. When provided, the server will return the next
     *   page of results strictly after the given appl_id (exclusive). This is a
     *   keyset/cursor-based pagination parameter that avoids OFFSET scans on large
     *   tables and gives faster, more consistent performance for deep paging.
     *
     * Example:
     * 1) Client requests first page: GET /api/map/customers?size=50
     *    Response includes { items: [...], after: "last-appl-id" }
     * 2) To fetch the following page use: GET /api/map/customers?after=last-appl-id&size=50
     *
     * Note: when both page and after are present, 'after' takes precedence.
     */
        public Map<String, Object> getCustomers(@RequestParam(value = "q", required = false) String q,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size,
            @RequestParam(value = "after", required = false) String after) {
        // Prefer keyset pagination for first page requests to avoid expensive COUNT/OFFSET scans.
        if (after != null && !after.isEmpty()) {
            return mapService.getCustomerListAfter(after, q, size);
        }
        if (page == 0) {
            // return initial page using keyset method for better performance; keep compatibility by
            // returning the same shape (items, size, after) that clients using keyset expect.
            return mapService.getCustomerListAfter(null, q, size);
        }
        return mapService.getCustomerListPaged(q, page, size);
    }

    @GetMapping("/addresses")
    public Map<String, Object> getAddresses(@RequestParam("applId") String applId,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return mapService.getAddressListPaged(applId, q, page, size);
    }

    @GetMapping(value = "/addresses/geojson")
        public ResponseEntity<JsonNode> getAddressesGeoJson(@RequestParam("applId") String applId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        try {
            JsonNode json = mapService.getAddressesGeoJsonByAppl(applId, page, size);
            if (json == null || json.isNull()) {
                ObjectNode fc = objectMapper.createObjectNode();
                fc.put("type", "FeatureCollection");
                fc.set("features", objectMapper.createArrayNode());
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(fc);
            }
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
        } catch (Exception e) {
            log.error("Error in getAddressesGeoJson for applId={}", applId, e);
            throw e;
        }
    }

    @GetMapping(value = "/checkins/geojson")
        public ResponseEntity<JsonNode> getCheckinsGeoJson(@RequestParam("applId") String applId,
            @RequestParam(value = "fcId", required = false) String fcId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        JsonNode json = mapService.getCheckinsGeoJsonByAppl(applId, fcId, page, size);
        if (json == null || json.isNull()) {
            ObjectNode fc = objectMapper.createObjectNode();
            fc.put("type", "FeatureCollection");
            fc.set("features", objectMapper.createArrayNode());
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(fc);
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
    }

    @GetMapping("/checkins/fcids")
    public Map<String, Object> getCheckinFcIds(@RequestParam("applId") String applId,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return mapService.getCheckinFcIdsPaged(applId, q, page, size);
    }

    @GetMapping("/search")
    public List<Map<String, Object>> search(@RequestParam("q") String q) {
        return mapService.searchLocation(q);
    }

    @GetMapping("/reverse")
    public Map<String, Object> reverseByPoint(@RequestParam("lon") double lon, @RequestParam("lat") double lat) {
        return mapService.reverseLookupByPoint(lon, lat);
    }

    @GetMapping("/reverse/address")
    public Map<String, Object> reverseByAddress(@RequestParam("addressId") String addressId) {
        return mapService.reverseLookupByAddressId(addressId);
    }

    @GetMapping(value = "/addresses/predict")
    public ResponseEntity<JsonNode> predictAddressLocation(@RequestParam("applId") String applId,
            @RequestParam("addressId") String addressId) {
        try {
            JsonNode json = mapService.predictAddressLocation(applId, addressId);
            if (json == null || json.isNull()) {
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(NullNode.instance);
            }
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
        } catch (Exception e) {
            log.error("Error in predictAddressLocation for applId={}, addressId={}", applId, addressId, e);
            throw e;
        }
    }

}

package vn.admin.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import vn.admin.service.MapService;

@RestController
@RequestMapping("/api/map")
public class MapController {

    @Autowired
    private MapService mapService;

    @GetMapping(value = "/districts/geojson", produces = "application/json; charset=UTF-8")
    public String getDistrictsGeoJsonByProvince(@RequestParam String provinceId) {
        return mapService.getDistrictsGeoJsonByProvince(provinceId);
    }

    @GetMapping("/province/bounds")
    public String getProvinceBounds(@RequestParam String provinceId) {
        return mapService.getProvinceBounds(provinceId);
    }

    @GetMapping(value = "/wards/geojson", produces = "application/json; charset=UTF-8")
    public String getWardsGeoJson(@RequestParam String districtId) {
        return mapService.getWardsGeoJsonByDistrict(districtId);
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
package vn.admin.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.NullNode;

@Service
public class MapService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String getSql() {
        return "SELECT jsonb_build_object(" + " 'type', 'FeatureCollection',"
                + " 'features', jsonb_agg(features.feature)" + ") " + "FROM (" + " SELECT jsonb_build_object("
                + " 'type', 'Feature', " + " 'geometry', ST_AsGeoJSON(geom_boundary)::jsonb, "
                + " 'properties', jsonb_build_object(" + " 'id', district_id, " + " 'name', name_vn" + " )"
                + " ) AS feature " + " FROM vn_districts " + " WHERE province_id = ?" + ") features";
    }

    public JsonNode getDistrictsGeoJsonByProvince(String provinceId) {
        String sql = "SELECT jsonb_build_object(" +
                "  'type', 'FeatureCollection'," +
                "  'province_name', (SELECT name_vn FROM vn_provinces WHERE province_id = ?)," +
                "  'features', jsonb_agg(features.feature)" +
                ") " +
                "FROM (" +
                "  SELECT jsonb_build_object(" +
                "    'type', 'Feature', " +
                "    'geometry', ST_AsGeoJSON(geom_boundary)::jsonb, " +
                "    'properties', jsonb_build_object(" +
                "        'id', district_id, " +
                "        'name', name_vn," +
                "        'center', ST_AsGeoJSON(ST_PointOnSurface(geom_boundary))::jsonb" +
                "    )" +
                "  ) AS feature " +
                "  FROM vn_districts " +
                "  WHERE province_id = ?" +
                ") features";
        try {
            String raw = jdbcTemplate.queryForObject(sql, String.class, provinceId, provinceId);
            if (raw == null) {
                ObjectNode fc = objectMapper.createObjectNode();
                fc.put("type", "FeatureCollection");
                fc.set("features", objectMapper.createArrayNode());
                return fc;
            }
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            ObjectNode fc = objectMapper.createObjectNode();
            fc.put("type", "FeatureCollection");
            fc.set("features", objectMapper.createArrayNode());
            return fc;
        }
    }

    public JsonNode getProvinceBounds(String provinceId) {
        // Sử dụng ST_AsGeoJSON trực tiếp trên geom_boundary, KHÔNG dùng ST_Envelope
        String sql = "SELECT ST_AsGeoJSON(geom_boundary) FROM vn_provinces WHERE province_id = ?";
        try {
            String raw = jdbcTemplate.queryForObject(sql, String.class, provinceId);
            if (raw == null || "null".equalsIgnoreCase(raw.trim())) {
                return NullNode.instance;
            }
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            return NullNode.instance;
        }
    }

    public JsonNode getWardsGeoJsonByDistrict(String districtId) {
        String sql = "SELECT jsonb_build_object(" +
                "  'type', 'FeatureCollection'," +
                "  'features', jsonb_agg(features.feature)" +
                ") " +
                "FROM (" +
                "  SELECT jsonb_build_object(" +
                "    'type',       'Feature', " +
                "    'geometry',   ST_AsGeoJSON(geom_boundary)::jsonb, " +
                "    'properties', jsonb_build_object(" +
                "        'id',     ward_id, " +
                "        'name',   name_vn, " +
                "        'center', ST_AsGeoJSON(ST_PointOnSurface(geom_boundary))::jsonb" +
                "    )" +
                "  ) AS feature " +
                "  FROM vn_wards " +
                "  WHERE district_id = ?" +
                ") features";
        try {
            String raw = jdbcTemplate.queryForObject(sql, String.class, districtId);
            if (raw == null) {
                ObjectNode fc = objectMapper.createObjectNode();
                fc.put("type", "FeatureCollection");
                fc.set("features", objectMapper.createArrayNode());
                return fc;
            }
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            ObjectNode fc = objectMapper.createObjectNode();
            fc.put("type", "FeatureCollection");
            fc.set("features", objectMapper.createArrayNode());
            return fc;
        }
    }

    public List<Map<String, Object>> getProvinceList() {
        String sql = "SELECT province_id as id, name_vn as name FROM vn_provinces ORDER BY name_vn";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getDistrictList(String provinceId) {
        String sql = "SELECT district_id as id, name_vn as name FROM vn_districts WHERE province_id = ? ORDER BY name_vn";
        return jdbcTemplate.queryForList(sql, provinceId);
    }

    public List<Map<String, Object>> getWardList(String districtId) {
        String sql = "SELECT ward_id as id, name_vn as name FROM vn_wards WHERE district_id = ? ORDER BY name_vn";
        return jdbcTemplate.queryForList(sql, districtId);
    }

    // Customers: distinct appl_id from customer_address
    public List<Map<String, Object>> getCustomerList() {
        String sql = "SELECT DISTINCT appl_id as id, appl_id as name FROM customer_address WHERE appl_id IS NOT NULL ORDER BY appl_id";
        return jdbcTemplate.queryForList(sql);
    }

    // Addresses for a given customer (list)
    public List<Map<String, Object>> getAddressList(String applId) {
        String sql = "SELECT id as id, address as name, address_type FROM customer_address WHERE appl_id = ? ORDER BY id";
        return jdbcTemplate.queryForList(sql, applId);
    }

    // Addresses as GeoJSON points (address_long, address_lat)
    public JsonNode getAddressesGeoJsonByAppl(String applId) {
        String sql = "SELECT jsonb_build_object('type', 'FeatureCollection', 'features', jsonb_agg(features.feature)) " +
                "FROM (" +
                "  SELECT jsonb_build_object('type','Feature', 'geometry', ST_AsGeoJSON(ST_SetSRID(ST_Point(address_long::double precision, address_lat::double precision), 4326))::jsonb, 'properties', jsonb_build_object('id', id, 'address', address, 'address_type', address_type)) AS feature " +
                "  FROM customer_address WHERE appl_id = ? AND address_lat IS NOT NULL AND address_long IS NOT NULL" +
                ") features";
        try {
            String raw = jdbcTemplate.queryForObject(sql, String.class, applId);
            if (raw == null) {
                ObjectNode fc = objectMapper.createObjectNode();
                fc.put("type", "FeatureCollection");
                fc.set("features", objectMapper.createArrayNode());
                return fc;
            }
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            ObjectNode fc = objectMapper.createObjectNode();
            fc.put("type", "FeatureCollection");
            fc.set("features", objectMapper.createArrayNode());
            return fc;
        }
    }

    // Checkins as GeoJSON points; optional fcId filter
    public JsonNode getCheckinsGeoJsonByAppl(String applId, String fcId) {
        String where = " WHERE appl_id = ? AND field_lat IS NOT NULL AND field_long IS NOT NULL";
        Object[] args;
        if (fcId != null && !fcId.isEmpty()) {
            where += " AND fc_id = ?";
            args = new Object[] { applId, fcId };
        } else {
            args = new Object[] { applId };
        }
        String sql = "SELECT jsonb_build_object('type','FeatureCollection','features', jsonb_agg(features.feature)) FROM (" +
                " SELECT jsonb_build_object('type','Feature', 'geometry', ST_AsGeoJSON(ST_SetSRID(ST_Point(field_long::double precision, field_lat::double precision),4326))::jsonb, 'properties', jsonb_build_object('id', id, 'appl_id', appl_id, 'fc_id', fc_id, 'customer_address_id', customer_address_id, 'checkin_date', checkin_date)) AS feature FROM checkin_address" +
                where + 
                ") features";
        try {
            String raw = (args.length == 2) ? jdbcTemplate.queryForObject(sql, String.class, args[0], args[1]) : jdbcTemplate.queryForObject(sql, String.class, args[0]);
            if (raw == null) {
                ObjectNode fc = objectMapper.createObjectNode();
                fc.put("type", "FeatureCollection");
                fc.set("features", objectMapper.createArrayNode());
                return fc;
            }
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            ObjectNode fc = objectMapper.createObjectNode();
            fc.put("type", "FeatureCollection");
            fc.set("features", objectMapper.createArrayNode());
            return fc;
        }
    }

    // Distinct fc_id values for a customer
    public List<Map<String, Object>> getCheckinFcIds(String applId) {
        String sql = "SELECT DISTINCT fc_id as id, fc_id as name FROM checkin_address WHERE appl_id = ? AND fc_id IS NOT NULL ORDER BY fc_id";
        return jdbcTemplate.queryForList(sql, applId);
    }

    public List<Map<String, Object>> searchLocation(String query) {
        String sql =
                // Tìm Tỉnh
                "SELECT province_id as id, name_vn as name, 'province' as type, null as parent_id, null as province_id FROM vn_provinces WHERE name_vn ILIKE ? "
                        +
                        "UNION ALL " +
                        // Tìm Huyện: Hiện "Huyện, Tỉnh"
                        "SELECT d.district_id as id, (d.name_vn || ', ' || p.name_vn) as name, 'district' as type, d.province_id as parent_id, d.province_id as province_id "
                        +
                        "FROM vn_districts d JOIN vn_provinces p ON d.province_id = p.province_id WHERE d.name_vn ILIKE ? "
                        +
                        "UNION ALL " +
                        // Tìm Xã: Hiện "Xã, Huyện, Tỉnh"
                        "SELECT w.ward_id as id, (w.name_vn || ', ' || d.name_vn || ', ' || p.name_vn) as name, 'ward' as type, w.district_id as parent_id, d.province_id as province_id "
                        +
                        "FROM vn_wards w JOIN vn_districts d ON w.district_id = d.district_id JOIN vn_provinces p ON d.province_id = p.province_id WHERE w.name_vn ILIKE ? "
                        +
                        "LIMIT 15";

        String p = "%" + query + "%";
        return jdbcTemplate.queryForList(sql, p, p, p);
    }
}

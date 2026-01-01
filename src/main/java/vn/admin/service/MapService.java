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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

@Service
public class MapService {
    private static final Logger log = LoggerFactory.getLogger(MapService.class);
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Simple in-memory cache for the very first page of customers (no 'after' and empty q)
    // This avoids repeating the same expensive initial read over and over in short bursts.
    private volatile Map<String, Object> customersFirstPageCache = null;
    private volatile long customersFirstPageCacheTs = 0L;
    // TTL for the first page cache in milliseconds. Default 5s to minimize staleness now that
    // the customers table + indexes are available and fast.
    private final long customersFirstPageCacheTtlMs;

    public MapService(@Value("${app.customersFirstPageCacheTtlMs:0}") long customersFirstPageCacheTtlMs) {
        this.customersFirstPageCacheTtlMs = customersFirstPageCacheTtlMs;
    }

    // No-arg constructor for tests that instantiate via reflection (e.g., Mockito @InjectMocks)
    public MapService() {
        this(0L);
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Helper: detect if a small 'customers' table exists to accelerate distinct appl_id queries
    private boolean hasCustomersTable() {
        try {
            Integer cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'customers'", Integer.class);
            return cnt != null && cnt > 0;
        } catch (Exception e) {
            return false;
        }
    }

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
            log.warn("getDistrictsGeoJsonByProvince failed for provinceId={}", provinceId, e);
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

    /**
     * Reverse geocode a point to administrative units (province, district, ward).
     * Returns a map with keys: provinceId, provinceName, districtId, districtName, wardId, wardName
     */
    public java.util.Map<String, Object> reverseLookupByPoint(double lon, double lat) {
        java.util.Map<String, Object> out = new java.util.HashMap<>();
        try {
            // First try ward (most specific)
            String wardSql = "SELECT w.ward_id as ward_id, w.name_vn as ward_name, d.district_id as district_id, d.name_vn as district_name, p.province_id as province_id, p.name_vn as province_name " +
                    "FROM vn_wards w JOIN vn_districts d ON w.district_id = d.district_id JOIN vn_provinces p ON d.province_id = p.province_id " +
                    "WHERE ST_Contains(w.geom_boundary, ST_SetSRID(ST_Point(?, ?), 4326)) LIMIT 1";
            java.util.List<java.util.Map<String, Object>> wardRows = jdbcTemplate.queryForList(wardSql, lon, lat);
            if (!wardRows.isEmpty()) {
                java.util.Map<String, Object> row = wardRows.get(0);
                out.put("wardId", row.get("ward_id"));
                out.put("wardName", row.get("ward_name"));
                out.put("districtId", row.get("district_id"));
                out.put("districtName", row.get("district_name"));
                out.put("provinceId", row.get("province_id"));
                out.put("provinceName", row.get("province_name"));
                return out;
            }

            // Try district next
            String distSql = "SELECT d.district_id as district_id, d.name_vn as district_name, p.province_id as province_id, p.name_vn as province_name " +
                    "FROM vn_districts d JOIN vn_provinces p ON d.province_id = p.province_id " +
                    "WHERE ST_Contains(d.geom_boundary, ST_SetSRID(ST_Point(?, ?), 4326)) LIMIT 1";
            java.util.List<java.util.Map<String, Object>> distRows = jdbcTemplate.queryForList(distSql, lon, lat);
            if (!distRows.isEmpty()) {
                java.util.Map<String, Object> row = distRows.get(0);
                out.put("districtId", row.get("district_id"));
                out.put("districtName", row.get("district_name"));
                out.put("provinceId", row.get("province_id"));
                out.put("provinceName", row.get("province_name"));
                return out;
            }

            // Finally, try province
            String provSql = "SELECT province_id as province_id, name_vn as province_name FROM vn_provinces WHERE ST_Contains(geom_boundary, ST_SetSRID(ST_Point(?, ?), 4326)) LIMIT 1";
            java.util.List<java.util.Map<String, Object>> provRows = jdbcTemplate.queryForList(provSql, lon, lat);
            if (!provRows.isEmpty()) {
                java.util.Map<String, Object> row = provRows.get(0);
                out.put("provinceId", row.get("province_id"));
                out.put("provinceName", row.get("province_name"));
                return out;
            }
        } catch (Exception e) {
            log.warn("reverseLookupByPoint failed for lon={}, lat={}", lon, lat, e);
        }
        return out;
    }

    /**
     * Reverse lookup for a customer address by address id.
     * Returns the same map as reverseLookupByPoint, or empty map if coordinates missing/not found.
     */
    public java.util.Map<String, Object> reverseLookupByAddressId(String addressId) {
        try {
            // Compare on id::text so we accept both numeric and string id types without type errors
            String sql = "SELECT address_long as lon, address_lat as lat FROM customer_address WHERE id::text = ? LIMIT 1";
            java.util.Map<String, Object> row = jdbcTemplate.queryForMap(sql, addressId);
            if (row != null && row.get("lon") != null && row.get("lat") != null) {
                double lon = Double.parseDouble(String.valueOf(row.get("lon")));
                double lat = Double.parseDouble(String.valueOf(row.get("lat")));
                return reverseLookupByPoint(lon, lat);
            }
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            // ignore
        } catch (Exception e) {
            log.warn("reverseLookupByAddressId failed for id={}", addressId, e);
        }
        return new java.util.HashMap<>();
    }

    // Customers: distinct appl_id from customer_address
    public List<Map<String, Object>> getCustomerList() {
        if (hasCustomersTable()) {
            String sql = "SELECT appl_id as id, appl_id as name FROM customers WHERE appl_id IS NOT NULL ORDER BY appl_id";
            return jdbcTemplate.queryForList(sql);
        }
        String sql = "SELECT DISTINCT appl_id as id, appl_id as name FROM customer_address WHERE appl_id IS NOT NULL ORDER BY appl_id";
        return jdbcTemplate.queryForList(sql);
    }

    // Paged customers with optional query
    public Map<String, Object> getCustomerListPaged(String q, int page, int size) {
        /**
         * Page through distinct customers (appl_id) using OFFSET/LIMIT.
         *
         * This method keeps legacy page/size semantics and returns a response
         * envelope with: { items, total, page, size, after }
         * - "after" is the appl_id of the last returned item and can be used
         *   by clients to switch to keyset pagination for faster subsequent pages.
         */
        // If a dedicated 'customers' table exists, use it (no DISTINCT/aggregates on the large table)
        if (hasCustomersTable()) {
            long start = System.currentTimeMillis();
            String where = " WHERE appl_id IS NOT NULL";
            java.util.ArrayList<Object> countParams = new java.util.ArrayList<>();
            java.util.ArrayList<Object> listParams = new java.util.ArrayList<>();
            if (q != null && !q.isEmpty()) {
                where += " AND appl_id ILIKE ?";
                countParams.add("%" + q + "%");
                listParams.add("%" + q + "%");
            }
            String countSql = "SELECT COUNT(*) FROM customers" + (where.isEmpty() ? "" : where);
            long total = jdbcTemplate.queryForObject(countSql, Long.class, countParams.toArray());

            String listSql = "SELECT appl_id as id, appl_id as name FROM customers" + where + " ORDER BY appl_id LIMIT ? OFFSET ?";
            listParams.add(size);
            listParams.add(page * size);
            List<Map<String, Object>> items = jdbcTemplate.queryForList(listSql, listParams.toArray());

            Map<String, Object> resp = new java.util.HashMap<>();
            resp.put("items", items);
            resp.put("total", total);
            resp.put("page", page);
            resp.put("size", size);
            if (!items.isEmpty()) {
                Object last = items.get(items.size() - 1).get("id");
                resp.put("after", last == null ? null : String.valueOf(last));
            } else {
                resp.put("after", null);
            }
            long took = System.currentTimeMillis() - start;
            log.info("getCustomerListPaged(customers table)(q={}, page={}, size={}) returned {} items in {} ms", q, page, size, items.size(), took);
            return resp;
        }

        // Fall back to legacy DISTINCT query when customers table is not present
        long start = System.currentTimeMillis();
        String where = " WHERE appl_id IS NOT NULL";
        Object[] paramsCount;
        Object[] paramsList;
        if (q != null && !q.isEmpty()) {
            where += " AND appl_id ILIKE ?";
            paramsCount = new Object[] { "%" + q + "%" };
            paramsList = new Object[] { "%" + q + "%", size, page * size };
        } else {
            paramsCount = new Object[] {};
            paramsList = new Object[] { size, page * size };
        }

        String countSql = "SELECT COUNT(*) FROM (SELECT DISTINCT appl_id FROM customer_address" + where + ") t";
        long total = (q != null && !q.isEmpty()) ? jdbcTemplate.queryForObject(countSql, Long.class, paramsCount)
                : jdbcTemplate.queryForObject(countSql, Long.class);

        String listSqlBase = "SELECT DISTINCT appl_id as id, appl_id as name FROM customer_address" + where + " ORDER BY appl_id";
        String listSql = "SELECT id, name FROM (" + listSqlBase + ") t LIMIT ? OFFSET ?";
        List<Map<String, Object>> items = (q != null && !q.isEmpty())
                ? jdbcTemplate.queryForList(listSql, paramsList)
                : jdbcTemplate.queryForList(listSql, paramsList);

        Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("items", items);
        resp.put("total", total);
        resp.put("page", page);
        resp.put("size", size);
        // provide an 'after' cursor (last appl_id) to support keyset pagination for faster subsequent pages
        if (!items.isEmpty()) {
            Object last = items.get(items.size() - 1).get("id");
            resp.put("after", last == null ? null : String.valueOf(last));
        } else {
            resp.put("after", null);
        }
        long took = System.currentTimeMillis() - start;
        log.info("getCustomerListPaged(q={}, page={}, size={}) returned {} items in {} ms", q, page, size, items.size(), took);
        return resp;
    }

    /**
     * Keyset (cursor) pagination: return the next page of distinct appl_id values
     * strictly after the provided `after` cursor value.
     *
     * - `after` is the last appl_id returned by a prior call. The query returns
     *   rows with appl_id > after ordered by appl_id. This avoids expensive
     *   OFFSET scans and keeps response times stable for deep paging.
     * - The response contains { items, size, after } where `after` is the new
     *   cursor (appl_id) to use for the following page (or null if no more items).
     */
    public Map<String, Object> getCustomerListAfter(String after, String q, int size) {
        long start = System.currentTimeMillis();
        // Serve simple cached response for empty 'after' and empty query to reduce repeated latency
        if ((after == null || after.isEmpty()) && (q == null || q.isEmpty())) {
            long now = System.currentTimeMillis();
            if (customersFirstPageCache != null && (now - customersFirstPageCacheTs) < customersFirstPageCacheTtlMs) {
                log.info("getCustomerListAfter(after={}, q={}, size={}) served from cache", after, q, size);
                return customersFirstPageCache;
            }
        }
        // Prefer the small customers table when available; it's much faster for keyset scans
        if (hasCustomersTable()) {
            java.util.ArrayList<Object> params = new java.util.ArrayList<>();
            String where = " WHERE appl_id IS NOT NULL";
            if (q != null && !q.isEmpty()) {
                where += " AND appl_id ILIKE ?";
                params.add("%" + q + "%");
            }
            if (after != null && !after.isEmpty()) {
                where += " AND appl_id > ?";
                params.add(after);
            }
            String sql = "SELECT appl_id as id, appl_id as name FROM customers" + where + " ORDER BY appl_id LIMIT ?";
            params.add(size);
            List<Map<String, Object>> items = jdbcTemplate.queryForList(sql, params.toArray());

            Map<String, Object> resp = new java.util.HashMap<>();
            resp.put("items", items);
            resp.put("size", size);
            if (!items.isEmpty()) {
                Object last = items.get(items.size() - 1).get("id");
                resp.put("after", last == null ? null : String.valueOf(last));
            } else {
                resp.put("after", null);
            }

            long took = System.currentTimeMillis() - start;
            log.info("getCustomerListAfter(customers table)(after={}, q={}, size={}) returned {} items in {} ms", after, q, size, items.size(), took);

            // populate cache for the empty initial page (only if TTL > 0)
            if ((after == null || after.isEmpty()) && (q == null || q.isEmpty()) && customersFirstPageCacheTtlMs > 0) {
                customersFirstPageCache = resp;
                customersFirstPageCacheTs = System.currentTimeMillis();
            }
            return resp;
        }

        String where = " WHERE appl_id IS NOT NULL";
        java.util.ArrayList<Object> params = new java.util.ArrayList<>();
        if (q != null && !q.isEmpty()) {
            where += " AND appl_id ILIKE ?";
            params.add("%" + q + "%");
        }
        if (after != null && !after.isEmpty()) {
            where += " AND appl_id > ?";
            params.add(after);
        }
        String sql = "SELECT DISTINCT appl_id as id, appl_id as name FROM customer_address" + where + " ORDER BY appl_id LIMIT ?";
        params.add(size);
        List<Map<String, Object>> items = jdbcTemplate.queryForList(sql, params.toArray());

        Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("items", items);
        resp.put("size", size);
        // set next 'after' cursor
        if (!items.isEmpty()) {
            Object last = items.get(items.size() - 1).get("id");
            resp.put("after", last == null ? null : String.valueOf(last));
        } else {
            resp.put("after", null);
        }
        long took = System.currentTimeMillis() - start;
        log.info("getCustomerListAfter(after={}, q={}, size={}) returned {} items in {} ms", after, q, size, items.size(), took);

        // populate cache for the empty initial page (only if TTL > 0)
        if ((after == null || after.isEmpty()) && (q == null || q.isEmpty()) && customersFirstPageCacheTtlMs > 0) {
            customersFirstPageCache = resp;
            customersFirstPageCacheTs = System.currentTimeMillis();
            log.info("getCustomerListAfter: cached first page for {} ms", customersFirstPageCacheTtlMs);
        }
        return resp;
    }

    /**
     * Clear the cached first page (useful for tests or manual invalidation).
     */
    public void clearCustomerFirstPageCache() {
        this.customersFirstPageCache = null;
        this.customersFirstPageCacheTs = 0L;
    }

    // Addresses for a given customer (list)
    public List<Map<String, Object>> getAddressList(String applId) {
        String sql = "SELECT id as id, address as name, address_type FROM customer_address WHERE appl_id = ? ORDER BY id";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, applId);
        // annotate each row with is_exact flag using heuristic
        for (Map<String, Object> r : rows) {
            Object addrObj = r.get("name");
            String addr = addrObj != null ? String.valueOf(addrObj) : null;
            boolean exact = isExactAddress(addr);
            // Final 'is_exact' requires DB-backed verification (checkins exist)
            Object idObj = r.get("id");
            String addrId = idObj == null ? null : String.valueOf(idObj);
            // Final is_exact requires DB-backed verification. We mark an address
            // as exact when there is authoritative evidence (e.g., checkins)
            // associated with that address id regardless of the syntactic
            // heuristic, per the DB-backed verification policy (Option 3).
            boolean verifiedExact = dbVerifyAddressById(addrId);
            r.put("is_exact", verifiedExact);
            // is_resolvable: true if syntactically exact OR if we can predict a location from checkins
            boolean resolvable = exact;
            com.fasterxml.jackson.databind.JsonNode pred = null;
            if (!resolvable) {
                try {
                    pred = predictAddressLocation(applId, addrId);
                    resolvable = pred != null && !pred.isNull();
                } catch (Exception e) {
                    // ignore prediction errors and leave resolvable as-is
                }
            }
            // copy prediction metadata when present
            if (pred != null && !pred.isNull() && pred.has("properties")) {
                try {
                    com.fasterxml.jackson.databind.JsonNode pp = pred.get("properties");
                    if (pp.has("confidence")) r.put("confidence", pp.get("confidence").asDouble(0.0));
                    if (pp.has("resolvability_reason")) r.put("resolvability_reason", pp.get("resolvability_reason").asText(""));
                    if (pp.has("inferred_province_id")) r.put("inferred_province_id", pp.get("inferred_province_id").asText(""));
                    if (pp.has("inferred_province_name")) r.put("inferred_province_name", pp.get("inferred_province_name").asText(""));
                    if (pp.has("inferred_district_id")) r.put("inferred_district_id", pp.get("inferred_district_id").asText(""));
                    if (pp.has("inferred_district_name")) r.put("inferred_district_name", pp.get("inferred_district_name").asText(""));
                    if (pp.has("inferred_ward_id")) r.put("inferred_ward_id", pp.get("inferred_ward_id").asText(""));
                    if (pp.has("inferred_ward_name")) r.put("inferred_ward_name", pp.get("inferred_ward_name").asText(""));
                    if (pp.has("verified")) r.put("verified", pp.get("verified").asBoolean(false));
                } catch (Exception e) { /* ignore metadata extraction */ }
            }
            r.put("is_resolvable", resolvable);
        }
        return rows;
    }

    // Paged addresses for a customer with optional query
    public Map<String, Object> getAddressListPaged(String applId, String q, int page, int size) {
        String where = " WHERE appl_id = ?";
        java.util.ArrayList<Object> countParams = new java.util.ArrayList<>();
        countParams.add(applId);
        if (q != null && !q.isEmpty()) {
            where += " AND address ILIKE ?";
            countParams.add("%" + q + "%");
        }
        String countSql = "SELECT COUNT(*) FROM customer_address" + where;
        long total = jdbcTemplate.queryForObject(countSql, Long.class, countParams.toArray());

        String listSql = "SELECT id as id, address as name, address_type FROM customer_address" + where + " ORDER BY id LIMIT ? OFFSET ?";
        java.util.ArrayList<Object> listParams = new java.util.ArrayList<>(countParams);
        listParams.add(size);
        listParams.add(page * size);
        List<Map<String, Object>> items = jdbcTemplate.queryForList(listSql, listParams.toArray());
        // annotate each returned item with is_exact flag
        for (Map<String, Object> item : items) {
            Object addrObj = item.get("name");
            String addr = addrObj != null ? String.valueOf(addrObj) : null;
            boolean exact = isExactAddress(addr);
            Object idObj = item.get("id");
            String addrId = idObj == null ? null : String.valueOf(idObj);
            // DB verification determines final exactness
            boolean verifiedExact = dbVerifyAddressById(addrId);
            item.put("is_exact", verifiedExact);
            // is_resolvable: true if syntactically exact or prediction from checkins exists
            boolean resolvable = exact;
            com.fasterxml.jackson.databind.JsonNode pred = null;
            if (!resolvable) {
                try {
                    pred = predictAddressLocation(applId, addrId);
                    resolvable = pred != null && !pred.isNull();
                } catch (Exception e) { /* ignore */ }
            }
            item.put("is_resolvable", resolvable);
            // copy prediction metadata into item when present
            if (pred != null && !pred.isNull() && pred.has("properties")) {
                try {
                    com.fasterxml.jackson.databind.JsonNode pp = pred.get("properties");
                    if (pp.has("confidence")) item.put("confidence", pp.get("confidence").asDouble(0.0));
                    if (pp.has("resolvability_reason")) item.put("resolvability_reason", pp.get("resolvability_reason").asText(""));
                    if (pp.has("inferred_province_id")) item.put("inferred_province_id", pp.get("inferred_province_id").asText(""));
                    if (pp.has("inferred_province_name")) item.put("inferred_province_name", pp.get("inferred_province_name").asText(""));
                    if (pp.has("inferred_district_id")) item.put("inferred_district_id", pp.get("inferred_district_id").asText(""));
                    if (pp.has("inferred_district_name")) item.put("inferred_district_name", pp.get("inferred_district_name").asText(""));
                    if (pp.has("inferred_ward_id")) item.put("inferred_ward_id", pp.get("inferred_ward_id").asText(""));
                    if (pp.has("inferred_ward_name")) item.put("inferred_ward_name", pp.get("inferred_ward_name").asText(""));
                    if (pp.has("verified")) item.put("verified", pp.get("verified").asBoolean(false));
                } catch (Exception e) { /* ignore */ }
            }
        }

        Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("items", items);
        resp.put("total", total);
        resp.put("page", page);
        resp.put("size", size);
        return resp;
    }

    // Addresses as GeoJSON points (address_long, address_lat)
    public JsonNode getAddressesGeoJsonByAppl(String applId) {
        // Default: return full set (keeps compatibility). Use getAddressesGeoJsonByAppl(applId, page, size)
        return getAddressesGeoJsonByAppl(applId, null, null);
    }

    public JsonNode getAddressesGeoJsonByAppl(String applId, Integer page, Integer size) {
        String where = " WHERE appl_id = ? AND address_lat IS NOT NULL AND address_long IS NOT NULL";
        Object[] countArgs = new Object[] { applId };
        String countSql = "SELECT COUNT(*) FROM customer_address" + where;
        long total = jdbcTemplate.queryForObject(countSql, Long.class, countArgs);

        String featuresSql = "SELECT jsonb_build_object('type','FeatureCollection','features', jsonb_agg(features.feature)" +
                (page != null && size != null ? ", 'meta', jsonb_build_object('total', ?, 'page', ?, 'size', ?)" : "") +
                ") FROM (" +
                "  SELECT jsonb_build_object('type','Feature', 'geometry', ST_AsGeoJSON(ST_SetSRID(ST_Point(address_long::double precision, address_lat::double precision), 4326))::jsonb, 'properties', jsonb_build_object('id', id, 'address', address, 'address_type', address_type)) AS feature " +
                "  FROM customer_address" + where;
        if (page != null && size != null) {
            featuresSql += " ORDER BY id LIMIT ? OFFSET ?";
            featuresSql += ") features";
            try {
                // total, page, size, applId, size, offset
                Object[] args = new Object[] { total, page, size, applId, size, page * size };
                String raw = jdbcTemplate.queryForObject(featuresSql, String.class, args);
                if (raw == null) return emptyFeatureCollection();
                // Add is_exact property to each feature's properties based on address string
                JsonNode root = objectMapper.readTree(raw);
                if (root != null && root.has("features") && root.get("features").isArray()) {
                    for (JsonNode f : root.withArray("features")) {
                        try {
                            JsonNode props = f.get("properties");
                                if (props != null && props.has("address")) {
                                String a = props.get("address").asText(null);
                                boolean syntacticExact = isExactAddress(a);
                                String aid = props.has("id") ? props.get("id").asText(null) : null;
                                // Final is_exact determined by DB-backed verification only (Option 3)
                                boolean verifiedExact = dbVerifyAddressById(aid);
                                try {
                                    log.info("getAddressesGeoJsonByAppl: applId={}, aid={}, verifiedExact={}", applId, aid, verifiedExact);
                                } catch (Exception ignore) { }
                                // Extra stdout for integration test visibility
                                try { System.out.println("DIAG_GEOJSON_VERIFY: applId=" + applId + " aid=" + aid + " verifiedExact=" + verifiedExact); } catch (Exception ignore) { }
                                ((com.fasterxml.jackson.databind.node.ObjectNode) props).put("is_exact", verifiedExact);
                                // ensure appl_id is present on each feature so UI tooltips show the application id
                                ((com.fasterxml.jackson.databind.node.ObjectNode) props).put("appl_id", applId);
                                // is_resolvable: if syntactically exact OR prediction exists via checkins
                                boolean exact = isExactAddress(a);
                                boolean resolvable = exact;
                                double confidence = 0.0;
                                String reason = "insufficient_locality";
                                if (!resolvable) {
                                    try {
                                        com.fasterxml.jackson.databind.JsonNode pred = predictAddressLocation(applId, aid);
                                        resolvable = pred != null && !pred.isNull();
                                        if (pred != null && !pred.isNull() && pred.has("properties")) {
                                            com.fasterxml.jackson.databind.JsonNode pp = pred.get("properties");
                                            if (pp.has("confidence")) confidence = pp.get("confidence").asDouble(0.0);
                                            if (pp.has("resolvability_reason")) reason = pp.get("resolvability_reason").asText("insufficient_locality");
                                            if (pp.has("inferred_province_id")) ((com.fasterxml.jackson.databind.node.ObjectNode) props).put("inferred_province_id", pp.get("inferred_province_id").asText(""));
                                            if (pp.has("inferred_province_name")) ((com.fasterxml.jackson.databind.node.ObjectNode) props).put("inferred_province_name", pp.get("inferred_province_name").asText(""));
                                            if (pp.has("inferred_district_id")) ((com.fasterxml.jackson.databind.node.ObjectNode) props).put("inferred_district_id", pp.get("inferred_district_id").asText(""));
                                            if (pp.has("inferred_district_name")) ((com.fasterxml.jackson.databind.node.ObjectNode) props).put("inferred_district_name", pp.get("inferred_district_name").asText(""));
                                            if (pp.has("inferred_ward_id")) ((com.fasterxml.jackson.databind.node.ObjectNode) props).put("inferred_ward_id", pp.get("inferred_ward_id").asText(""));
                                            if (pp.has("inferred_ward_name")) ((com.fasterxml.jackson.databind.node.ObjectNode) props).put("inferred_ward_name", pp.get("inferred_ward_name").asText(""));
                                            if (pp.has("verified")) ((com.fasterxml.jackson.databind.node.ObjectNode) props).put("verified", pp.get("verified").asBoolean(false));
                                            // Also embed the predicted feature and geometry so clients can render predicted points without an extra request
                                            try {
                                                ((com.fasterxml.jackson.databind.node.ObjectNode) props).set("predicted_feature", pred);
                                                if (pred.has("geometry")) ((com.fasterxml.jackson.databind.node.ObjectNode) props).set("predicted_geometry", pred.get("geometry"));
                                            } catch (Exception e) { /* ignore embedding issues */ }
                                        }
                                    } catch (Exception e) { /* ignore */ }
                                }
                                ((com.fasterxml.jackson.databind.node.ObjectNode) props).put("is_resolvable", resolvable);
                                ((com.fasterxml.jackson.databind.node.ObjectNode) props).put("confidence", confidence);
                                ((com.fasterxml.jackson.databind.node.ObjectNode) props).put("resolvability_reason", reason);
                                }
                        } catch (Exception e) { /* ignore per-feature */ }
                    }
                }
                return root;
            } catch (Exception e) {
                 log.warn("getAddressesGeoJsonByAppl failed for applId={}", applId, e);
                 return emptyFeatureCollection();
            }
        } else {
            featuresSql += ") features";
            try {
                String raw = jdbcTemplate.queryForObject(featuresSql, String.class, applId);
                if (raw == null) return emptyFeatureCollection();
                    JsonNode root = objectMapper.readTree(raw);
                    if (root != null && root.has("features") && root.get("features").isArray()) {
                        for (JsonNode f : root.withArray("features")) {
                            try {
                                JsonNode props = f.get("properties");
                                if (props != null && props.has("address")) {
                                    String a = props.get("address").asText(null);
                                            boolean syntacticExact = isExactAddress(a);
                                            String aid = props.has("id") ? props.get("id").asText(null) : null;
                                            // Final is_exact determined by DB-backed verification only (Option 3)
                                            boolean verifiedExact = dbVerifyAddressById(aid);
                                            try {
                                                log.info("getAddressesGeoJsonByAppl: applId={}, aid={}, verifiedExact={}", applId, aid, verifiedExact);
                                            } catch (Exception ignore) { }
                                            try { System.out.println("DIAG_GEOJSON_VERIFY: applId=" + applId + " aid=" + aid + " verifiedExact=" + verifiedExact); } catch (Exception ignore) { }
                                            ((com.fasterxml.jackson.databind.node.ObjectNode) props).put("is_exact", verifiedExact);
                                        // ensure appl_id is present on each feature so UI tooltips show the application id
                                        ((com.fasterxml.jackson.databind.node.ObjectNode) props).put("appl_id", applId);
                                            boolean resolvable = syntacticExact;
                                        double confidence = 0.0;
                                        String reason = "insufficient_locality";
                                        if (!resolvable) {
                                            try {
                                                    com.fasterxml.jackson.databind.JsonNode pred = predictAddressLocation(applId, aid);
                                                    resolvable = pred != null && !pred.isNull();
                                                    if (pred != null && !pred.isNull() && pred.has("properties")) {
                                                        com.fasterxml.jackson.databind.JsonNode pp = pred.get("properties");
                                                    if (pp.has("confidence")) confidence = pp.get("confidence").asDouble(0.0);
                                                    if (pp.has("resolvability_reason")) reason = pp.get("resolvability_reason").asText("insufficient_locality");
                                                    if (pp.has("inferred_province_id")) ((com.fasterxml.jackson.databind.node.ObjectNode) props).put("inferred_province_id", pp.get("inferred_province_id").asText(""));
                                                    if (pp.has("inferred_province_name")) ((com.fasterxml.jackson.databind.node.ObjectNode) props).put("inferred_province_name", pp.get("inferred_province_name").asText(""));
                                                    if (pp.has("inferred_district_id")) ((com.fasterxml.jackson.databind.node.ObjectNode) props).put("inferred_district_id", pp.get("inferred_district_id").asText(""));
                                                    if (pp.has("inferred_district_name")) ((com.fasterxml.jackson.databind.node.ObjectNode) props).put("inferred_district_name", pp.get("inferred_district_name").asText(""));
                                                    if (pp.has("inferred_ward_id")) ((com.fasterxml.jackson.databind.node.ObjectNode) props).put("inferred_ward_id", pp.get("inferred_ward_id").asText(""));
                                                    if (pp.has("inferred_ward_name")) ((com.fasterxml.jackson.databind.node.ObjectNode) props).put("inferred_ward_name", pp.get("inferred_ward_name").asText(""));
                                                    if (pp.has("verified")) ((com.fasterxml.jackson.databind.node.ObjectNode) props).put("verified", pp.get("verified").asBoolean(false));
                                                        // also embed predicted feature and geometry to the properties
                                                        try {
                                                            ((com.fasterxml.jackson.databind.node.ObjectNode) props).set("predicted_feature", pred);
                                                            if (pred.has("geometry")) ((com.fasterxml.jackson.databind.node.ObjectNode) props).set("predicted_geometry", pred.get("geometry"));
                                                        } catch (Exception e) { /* ignore */ }
                                                }
                                            } catch (Exception e) { /* ignore per-feature */ }
                                        }
                                        ((com.fasterxml.jackson.databind.node.ObjectNode) props).put("is_resolvable", resolvable);
                                        ((com.fasterxml.jackson.databind.node.ObjectNode) props).put("confidence", confidence);
                                        ((com.fasterxml.jackson.databind.node.ObjectNode) props).put("resolvability_reason", reason);
                                }
                            } catch (Exception e) { /* ignore per-feature */ }
                        }
                    }
                    return root;
            } catch (Exception e) {
                return emptyFeatureCollection();
            }
        }
    }

        /**
         * Heuristic to detect whether an address string is an "exact" address
         * (contains house number and street information) vs a non-exact
         * administrative-only description (province/district/ward only).
         */
        public boolean isExactAddress(String address) {
            if (address == null) return false;
            String s = address.trim().toLowerCase();
            if (s.isEmpty()) return false;
            // Administrative keywords are important to detect pure-admin strings
            boolean adminTokenPresent = s.matches(".*\\b(huyện|quận|phường|xã|tỉnh|thành phố|thị xã)\\b.*");
            // Normalize and detect numeric/structural cues
            boolean hasNumber = s.matches(".*\\d.*");
            boolean hasHousePrefix = s.matches(".*\\b(số|so)\\s*\\d+.*");
            boolean numberWithSlash = s.matches(".*\\d+\\s*\\/\\s*\\d+.*");
            // Street keywords: include explicit street/road markers but exclude alley-like tokens
            boolean hasStreetKeyword = s.matches(".*\\b(đường|duong|phố|pho|khu phố|khu pho|đ|d)\\b.*");
            // Alley tokens (ngõ/hẻm/ngách) are ambiguous — if the pattern is like 'ngõ 12 Nguyễn Trãi'
            // treat as non-exact unless an explicit house prefix ('số') or a compound number is present.
            boolean hasAlleyNumber = s.matches(".*\\b(ngõ|ngo|hẻm|hem|ngách|ngach)\\s*\\d+.*");

            // Only treat an address as syntactically 'exact' when it contains a numeric house
            // that appears *before* an explicit street token (e.g., '12 Đường Nguyễn Trãi')
            // AND additionally contains an administrative locality token (ward/district/province)
            // because a plain house+street without a locality is often ambiguous in the dataset.
            // Alley numbers (ngõ/hẻm/ngách) remain non-exact.
            if (hasAlleyNumber) return false;
            boolean numberBeforeStreet = s.matches(".*\\b\\d+(?:\\s*\\/\\s*\\d+)?\\b\\s+(đường|duong|phố|pho|khu phố|khu pho|đ|d)\\b.*");
            // Determine if admin token is sufficiently specific.
            // - Ward-level tokens (phường/xã) are specific enough.
            // - Province-level tokens (tỉnh/thành phố/thị xã) are specific enough.
            // - District tokens (quận/huyện) are NOT sufficient on their own because
            //   they are ambiguous without a province (e.g., 'Quận 1' appears in multiple cities).
            boolean hasWard = s.matches(".*\\b(phường|xã)\\b.*");
            boolean hasProvince = s.matches(".*\\b(tỉnh|thành phố|thị xã|tp|tp\\.|tp)\\b.*");
            boolean hasDistrict = s.matches(".*\\b(quận|huyện)\\b.*");

            // Exact only when number-before-street AND province is present AND
            // ward is present AND an explicit house prefix ('số') is used.
            // This is deliberately strict: syntactic exactness alone is a weak
            // signal and we prefer to require a clear explicit form before
            // considering the address syntactically exact. A DB-backed
            // verification step is still required for the final `is_exact` flag.
            if (numberBeforeStreet && hasProvince && hasWard && hasHousePrefix) return true;
            // If it's purely administrative (admin token present but no number-before-street)
            // keep it non-exact.
            if (adminTokenPresent && !numberBeforeStreet) return false;
            return false;
        }

        /**
         * DB-backed verification: check whether this address id has any checkins
         * associated with it. This is a conservative proxy for 'this address is
         * known in the database / seen in the wild'. The query accepts addressId
         * as text to avoid integer conversion problems in tests/requests.
         */
        private boolean dbVerifyAddressById(String addressId) {
            if (addressId == null) return false;
            try {
                // Parse the id into an integer to avoid driver-specific issues with casting
                int aid = Integer.parseInt(addressId);
                Long cnt = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM checkin_address WHERE customer_address_id = ?", Long.class, aid);
                return cnt != null && cnt > 0;
            } catch (NumberFormatException nfe) {
                return false;
            } catch (Exception e) {
                // Any DB error -> conservative false
                return false;
            }
        }

    private JsonNode emptyFeatureCollection() {
        ObjectNode fc = objectMapper.createObjectNode();
        fc.put("type", "FeatureCollection");
        fc.set("features", objectMapper.createArrayNode());
        return fc;
    }

    // Checkins as GeoJSON points; optional fcId filter
    public JsonNode getCheckinsGeoJsonByAppl(String applId, String fcId) {
        return getCheckinsGeoJsonByAppl(applId, fcId, null, null);
    }

    public JsonNode getCheckinsGeoJsonByAppl(String applId, String fcId, Integer page, Integer size) {
        // Use an aliased WHERE for feature SQL (needs 'c.' prefixes) and a plain WHERE for the count query
        String where = " WHERE c.appl_id = ? AND c.field_lat IS NOT NULL AND c.field_long IS NOT NULL";
        String whereCount = " WHERE appl_id = ? AND field_lat IS NOT NULL AND field_long IS NOT NULL";
        java.util.ArrayList<Object> countArgs = new java.util.ArrayList<>();
        countArgs.add(applId);
        if (fcId != null && !fcId.isEmpty()) {
            where += " AND fc_id = ?";
            whereCount += " AND fc_id = ?";
            countArgs.add(fcId);
        }
        String countSql = "SELECT COUNT(*) FROM checkin_address" + whereCount;
        long total = jdbcTemplate.queryForObject(countSql, Long.class, countArgs.toArray());

        // Enrich checkin features with stored/normalized distance and administrative info
        // The SQL builds GeoJSON Features and left-joins admin tables based on point containment.
        String featuresSql = "SELECT jsonb_build_object('type','FeatureCollection','features', jsonb_agg(features.feature)" +
            (page != null && size != null ? ", 'meta', jsonb_build_object('total', ?, 'page', ?, 'size', ?)" : "") +
            ") FROM (" +
            " SELECT jsonb_build_object('type','Feature', " +
            " 'geometry', ST_AsGeoJSON(ST_SetSRID(ST_Point(c.field_long::double precision, c.field_lat::double precision),4326))::jsonb, " +
            " 'properties', jsonb_build_object(" +
            "    'id', c.id, 'appl_id', c.appl_id, 'fc_id', c.fc_id, 'customer_address_id', c.customer_address_id, 'checkin_date', c.checkin_date, " +
            // distance: prefer persisted value, otherwise compute from linked customer address when available
            "    'distance', COALESCE(c.distance, ROUND(ST_Distance(ST_SetSRID(ST_Point(c.field_long::double precision, c.field_lat::double precision),4326)::geography, ST_SetSRID(ST_Point(a.address_long::double precision, a.address_lat::double precision),4326)::geography)::numeric,0)::double precision), " +
            "    'provinceId', p.province_id, 'provinceName', p.name_vn, 'districtId', d.district_id, 'districtName', d.name_vn, 'wardId', w.ward_id, 'wardName', w.name_vn " +
            " ) ) AS feature FROM checkin_address c " +
            " LEFT JOIN customer_address a ON c.customer_address_id = a.id " +
            " LEFT JOIN vn_wards w ON ST_Contains(w.geom_boundary, ST_SetSRID(ST_Point(c.field_long::double precision, c.field_lat::double precision),4326)) " +
            " LEFT JOIN vn_districts d ON (w.district_id = d.district_id) " +
            " LEFT JOIN vn_provinces p ON (d.province_id = p.province_id)" +
            where;

        if (page != null && size != null) {
            featuresSql += " ORDER BY id LIMIT ? OFFSET ?";
            featuresSql += ") features";
            try {
                java.util.ArrayList<Object> args = new java.util.ArrayList<>();
                args.add(total);
                args.add(page);
                args.add(size);
                args.addAll(countArgs);
                args.add(size);
                args.add(page * size);
                String raw = jdbcTemplate.queryForObject(featuresSql, String.class, args.toArray());
                if (raw == null) return emptyFeatureCollection();
                return objectMapper.readTree(raw);
            } catch (Exception e) {
                return emptyFeatureCollection();
            }
        } else {
            featuresSql += ") features";
            try {
                String raw = jdbcTemplate.queryForObject(featuresSql, String.class, countArgs.toArray());
                if (raw == null) return emptyFeatureCollection();
                return objectMapper.readTree(raw);
            } catch (Exception e) {
                 log.warn("getCheckinsGeoJsonByAppl failed for applId={}, fcId={}", applId, fcId, e);
                 return emptyFeatureCollection();
            }
        }
    }

    // Distinct fc_id values for a customer
    public List<Map<String, Object>> getCheckinFcIds(String applId) {
        String sql = "SELECT DISTINCT fc_id as id, fc_id as name FROM checkin_address WHERE appl_id = ? AND fc_id IS NOT NULL ORDER BY fc_id";
        return jdbcTemplate.queryForList(sql, applId);
    }

    // Paged fc_id values (searchable)
    public Map<String, Object> getCheckinFcIdsPaged(String applId, String q, int page, int size) {
        String where = " WHERE appl_id = ? AND fc_id IS NOT NULL";
        java.util.ArrayList<Object> params = new java.util.ArrayList<>();
        params.add(applId);
        if (q != null && !q.isEmpty()) {
            where += " AND fc_id ILIKE ?";
            params.add("%" + q + "%");
        }
        String countSql = "SELECT COUNT(DISTINCT fc_id) FROM checkin_address" + where;
        long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());

        String listSql = "SELECT DISTINCT fc_id as id, fc_id as name FROM checkin_address" + where + " ORDER BY fc_id LIMIT ? OFFSET ?";
        params.add(size);
        params.add(page * size);
        List<Map<String, Object>> items = jdbcTemplate.queryForList(listSql, params.toArray());

        Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("items", items);
        resp.put("total", total);
        resp.put("page", page);
        resp.put("size", size);
        return resp;
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

    /**
     * Predict a customer's address location based on checkin points for the
     * corresponding customer_address id. The method computes the centroid of
     * all checkins linked to the provided address id and, if an administrative
     * area can be inferred from the address string, ensures the returned point
     * lies within that area (otherwise returns a point on surface of the area).
     *
     * Returns a GeoJSON Feature (Point) with properties: applId, addressId,
     * adjusted (true if moved into the area), areaLevel (ward/district/province or empty).
     */
    public com.fasterxml.jackson.databind.JsonNode predictAddressLocation(String applId, String addressId) {
        try {
            // Compute centroid of checkins for this customer address id
            // Accept addressId passed as text by casting to integer so controller request params (strings)
            // work without causing "integer = character varying" errors in some Postgres setups.
            String csql = "SELECT ST_AsGeoJSON(ST_Centroid(ST_Collect(ST_SetSRID(ST_Point(field_long::double precision, field_lat::double precision),4326)))) FROM checkin_address WHERE customer_address_id = ?::int";
            String centroidRaw = null;
            try {
                centroidRaw = jdbcTemplate.queryForObject(csql, String.class, addressId);
            } catch (Exception e) {
                // keep centroidRaw as null and continue to allow DB-backed verification fallback
                centroidRaw = null;
            }

            // Attempt to infer administrative area from the address text (ward -> district -> province)
            String addrSql = "SELECT address FROM customer_address WHERE id::text = ? LIMIT 1";
            String addrText = null;
            try { addrText = jdbcTemplate.queryForObject(addrSql, String.class, addressId); } catch (Exception e) { /* ignore */ }

            String areaGeoJson = null;
            String areaLevel = null;
            if (addrText != null) {
                try {
                    String wardSql = "SELECT ST_AsGeoJSON(geom_boundary) FROM vn_wards WHERE ? ILIKE '%' || name_vn || '%' LIMIT 1";
                    areaGeoJson = jdbcTemplate.queryForObject(wardSql, String.class, addrText);
                    if (areaGeoJson != null) areaLevel = "ward";
                } catch (Exception e) { areaGeoJson = null; }
                if (areaGeoJson == null) {
                    try {
                        String distSql = "SELECT ST_AsGeoJSON(geom_boundary) FROM vn_districts WHERE ? ILIKE '%' || name_vn || '%' LIMIT 1";
                        areaGeoJson = jdbcTemplate.queryForObject(distSql, String.class, addrText);
                        if (areaGeoJson != null) areaLevel = "district";
                    } catch (Exception e) { areaGeoJson = null; }
                }
                if (areaGeoJson == null) {
                    try {
                        String provSql = "SELECT ST_AsGeoJSON(geom_boundary) FROM vn_provinces WHERE ? ILIKE '%' || name_vn || '%' LIMIT 1";
                        areaGeoJson = jdbcTemplate.queryForObject(provSql, String.class, addrText);
                        if (areaGeoJson != null) areaLevel = "province";
                    } catch (Exception e) { areaGeoJson = null; }
                }
            }

            String predictedGeoJson = centroidRaw;
            boolean adjusted = false;
            // If no centroid was computed, try a DB-backed fallback: if checkins exist, pick any checkin point
            if (predictedGeoJson == null) {
                try {
                    boolean dbvFallback = dbVerifyAddressById(addressId);
                    if (dbvFallback) {
                        String firstPointSql = "SELECT ST_AsGeoJSON(ST_SetSRID(ST_Point(field_long::double precision, field_lat::double precision),4326)) FROM checkin_address WHERE customer_address_id = ?::int LIMIT 1";
                        String firstPoint = jdbcTemplate.queryForObject(firstPointSql, String.class, addressId);
                        if (firstPoint != null) {
                            predictedGeoJson = firstPoint;
                            adjusted = false;
                        }
                    }
                } catch (Exception e) { /* ignore fallback failures */ }
            }
            if (areaGeoJson != null) {
                // First check whether the centroid is already inside the inferred administrative area
                Boolean contains = jdbcTemplate.queryForObject("SELECT ST_Contains(ST_SetSRID(ST_GeomFromGeoJSON(?),4326), ST_SetSRID(ST_GeomFromGeoJSON(?),4326))", Boolean.class, areaGeoJson, centroidRaw);
                if (contains != null && contains) {
                    // inside: keep centroid
                    predictedGeoJson = centroidRaw;
                    adjusted = false;
                } else {
                    // outside or unknown: compute a fallback point on the area's surface
                    String adjSql = "SELECT ST_AsGeoJSON(ST_PointOnSurface(ST_SetSRID(ST_GeomFromGeoJSON(?),4326)))";
                    String adj = jdbcTemplate.queryForObject(adjSql, String.class, areaGeoJson);
                    if (adj != null) predictedGeoJson = adj;
                    adjusted = true;
                }
            }

            // Build GeoJSON Feature output
            com.fasterxml.jackson.databind.node.ObjectNode feature = objectMapper.createObjectNode();
            feature.put("type", "Feature");
            feature.set("geometry", objectMapper.readTree(predictedGeoJson));
            com.fasterxml.jackson.databind.node.ObjectNode props = objectMapper.createObjectNode();
            // Use snake_case property name for consistency with other feature props used by the UI
            props.put("appl_id", applId);
            props.put("addressId", addressId);
            props.put("adjusted", adjusted);
            props.put("areaLevel", areaLevel == null ? "" : areaLevel);

            // Inferred administrative units based on predicted point
            try {
                com.fasterxml.jackson.databind.JsonNode inferred = reverseLookupByPointAsJson(predictedGeoJson);
                if (inferred != null && !inferred.isEmpty(null) && !inferred.isNull()) {
                    if (inferred.has("provinceId")) props.put("inferred_province_id", inferred.get("provinceId").asText(""));
                    if (inferred.has("provinceName")) props.put("inferred_province_name", inferred.get("provinceName").asText(""));
                    if (inferred.has("districtId")) props.put("inferred_district_id", inferred.get("districtId").asText(""));
                    if (inferred.has("districtName")) props.put("inferred_district_name", inferred.get("districtName").asText(""));
                    if (inferred.has("wardId")) props.put("inferred_ward_id", inferred.get("wardId").asText(""));
                    if (inferred.has("wardName")) props.put("inferred_ward_name", inferred.get("wardName").asText(""));
                }
            } catch (Exception e) { /* ignore inference issues */ }

                // Debug logging to help trace verification issues in integration tests
                try {
                    log.info("predictAddressLocation debug: applId={}, addressId={}, centroidPresent={}, dbVerify={}", applId, addressId, centroidRaw != null, dbVerifyAddressById(addressId));
                } catch (Exception ignore) { }
                // Additional diagnostics printed to stdout to ensure visibility in test runs
                try {
                    System.out.println("DIAG_SERVICE: applId=" + applId + " addressId=" + addressId + " centroidRaw=" + centroidRaw + " areaGeoJson=" + areaGeoJson + "");
                    boolean dbv = dbVerifyAddressById(addressId);
                    System.out.println("DIAG_SERVICE_DBVERIFY: " + dbv);
                } catch (Exception ignore) { }

            // Compute a lightweight confidence score and reason
            double confidence = 0.0;
            String reason = "insufficient_locality";
            boolean syntacticExact = isExactAddress(addrText);
            if (syntacticExact) {
                confidence += 0.6;
                reason = "syntactic_exact";
            }
            if (centroidRaw != null) {
                confidence += 0.3;
                if (!"syntactic_exact".equals(reason)) reason = "predicted_from_checkins";
            }
            if (areaGeoJson != null) {
                try {
                    Boolean containsCentroid = jdbcTemplate.queryForObject("SELECT ST_Contains(ST_SetSRID(ST_GeomFromGeoJSON(?),4326), ST_SetSRID(ST_GeomFromGeoJSON(?),4326))", Boolean.class, areaGeoJson, predictedGeoJson);
                    if (containsCentroid != null && containsCentroid) {
                        confidence += 0.1;
                        if (!reason.startsWith("syntactic")) reason = "predicted_in_area";
                    }
                } catch (Exception e) { /* ignore */ }
            }
            if (confidence > 1.0) confidence = 1.0;
            props.put("confidence", confidence);
            props.put("resolvability_reason", reason);

            // Verified: conservative DB-backed verification OR syntactic+area containment
            // If we computed a centroid from checkins above, treat the prediction as verified
            boolean verified = false;
            try {
                boolean dbv = dbVerifyAddressById(addressId);
                if (centroidRaw != null) {
                    verified = true;
                } else if (dbv) {
                    verified = true;
                } else if (syntacticExact && areaGeoJson != null) {
                    Boolean containsCentroid = jdbcTemplate.queryForObject("SELECT ST_Contains(ST_SetSRID(ST_GeomFromGeoJSON(?),4326), ST_SetSRID(ST_GeomFromGeoJSON(?),4326))", Boolean.class, areaGeoJson, predictedGeoJson);
                    verified = containsCentroid != null && containsCentroid;
                }
            } catch (Exception e) { /* ignore */ }
            props.put("verified", verified);
            feature.set("properties", props);
            try {
                log.info("predictAddressLocation returning feature for applId={}, addressId={} props={}", applId, addressId, props.toString());
            } catch (Exception ignore) { }
            return feature;
        } catch (Exception e) {
            log.warn("predictAddressLocation failed for applId={}, addressId={}", applId, addressId, e);
            return NullNode.instance;
        }
    }

    // Helper: run reverseLookupByPoint but return a simple JsonNode of province/district/ward
    private com.fasterxml.jackson.databind.JsonNode reverseLookupByPointAsJson(String pointGeoJson) {
        try {
            // parse coordinates from pointGeoJson and call reverseLookupByPoint
            com.fasterxml.jackson.databind.JsonNode g = objectMapper.readTree(pointGeoJson);
            if (g == null || !g.has("coordinates")) return NullNode.instance;
            com.fasterxml.jackson.databind.JsonNode coords = g.get("coordinates");
            double lon = coords.get(0).asDouble();
            double lat = coords.get(1).asDouble();
            java.util.Map<String, Object> m = reverseLookupByPoint(lon, lat);
            com.fasterxml.jackson.databind.node.ObjectNode out = objectMapper.createObjectNode();
            if (m.containsKey("provinceId")) out.put("provinceId", String.valueOf(m.get("provinceId")));
            if (m.containsKey("provinceName")) out.put("provinceName", String.valueOf(m.get("provinceName")));
            if (m.containsKey("districtId")) out.put("districtId", String.valueOf(m.get("districtId")));
            if (m.containsKey("districtName")) out.put("districtName", String.valueOf(m.get("districtName")));
            if (m.containsKey("wardId")) out.put("wardId", String.valueOf(m.get("wardId")));
            if (m.containsKey("wardName")) out.put("wardName", String.valueOf(m.get("wardName")));
            return out;
        } catch (Exception e) {
            return NullNode.instance;
        }
    }
}

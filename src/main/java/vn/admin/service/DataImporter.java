package vn.admin.service;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.google.gson.*;

import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;

@Service
public class DataImporter {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void importGadmData(String filePath) throws Exception {
        importGadmData(filePath, false);
    }

    public void importGadmData(String filePath, boolean dryRun) throws Exception {
        GeoJsonReader reader = new GeoJsonReader();
        JsonObject jsonContent = JsonParser.parseReader(new FileReader(filePath)).getAsJsonObject();
        JsonArray features = jsonContent.getAsJsonArray("features");

        Set<String> processedProvinces = new HashSet<>();
        Set<String> processedDistricts = new HashSet<>();

        for (JsonElement featureElement : features) {
            JsonObject feature = featureElement.getAsJsonObject();
            JsonObject props = feature.getAsJsonObject("properties");
            String geomStr = feature.getAsJsonObject("geometry").toString();

            // Lấy thông tin cấp bậc
            String pId = props.get("GID_1").getAsString();
            String pName = props.get("NAME_1").getAsString();
            String dId = props.get("GID_2").getAsString();
            String dName = props.get("NAME_2").getAsString();
            String wId = props.get("GID_3").getAsString();
            String wName = props.get("NAME_3").getAsString();

            // 1. Nạp Tỉnh (nếu chưa có)
            if (processedProvinces.add(pId)) {
                if (!dryRun) {
                    jdbcTemplate.update(
                            "INSERT INTO vn_provinces (province_id, name_vn) VALUES (?, ?) ON CONFLICT DO NOTHING", pId,
                            pName);
                }
            }

            // 2. Nạp Huyện (nếu chưa có)
            if (processedDistricts.add(dId)) {
                if (!dryRun) {
                    jdbcTemplate.update(
                            "INSERT INTO vn_districts (district_id, province_id, name_vn) VALUES (?, ?, ?) ON CONFLICT DO NOTHING",
                            dId, pId, dName);
                }
            }

            // 3. Nạp Xã & Cập nhật ranh giới/tọa độ bằng SQL PostGIS
            // Chúng ta dùng ST_GeomFromGeoJSON để PostGIS tự xử lý chuỗi Geometry
            String sqlWard = """
                        INSERT INTO vn_wards (ward_id, district_id, name_vn, geom_boundary, center_geom)
                        VALUES (?, ?, ?, ST_Multi(ST_GeomFromGeoJSON(?)), ST_PointOnSurface(ST_GeomFromGeoJSON(?)))
                        ON CONFLICT (ward_id) DO UPDATE SET geom_boundary = EXCLUDED.geom_boundary;
                    """;
            if (!dryRun) jdbcTemplate.update(sqlWard, wId, dId, wName, geomStr, geomStr);
        }

        // Cập nhật ranh giới gộp cho Huyện và Tỉnh từ dữ liệu Xã
        updateUpperLevels(dryRun);
    }

    private void updateUpperLevels(boolean dryRun) {
        System.out.println("⏳ Đang gộp ranh giới Tỉnh/Huyện...");
        if (!dryRun) jdbcTemplate.execute(
                "UPDATE vn_districts d SET geom_boundary = (SELECT ST_Union(geom_boundary) FROM vn_wards w WHERE w.district_id = d.district_id)");
        if (!dryRun) jdbcTemplate.execute(
                "UPDATE vn_provinces p SET geom_boundary = (SELECT ST_Union(geom_boundary) FROM vn_districts d WHERE d.province_id = p.province_id)");
        // Tạo điểm đặt nhãn chuẩn
        if (!dryRun) jdbcTemplate.execute("UPDATE vn_districts SET geom_label = ST_PointOnSurface(geom_boundary)");
        if (!dryRun) jdbcTemplate.execute("UPDATE vn_provinces SET geom_label = ST_PointOnSurface(geom_boundary)");
    }
}

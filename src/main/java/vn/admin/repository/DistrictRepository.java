package vn.admin.repository;

import vn.admin.entity.District;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface DistrictRepository extends JpaRepository<District, String> {
    // Tìm các huyện thuộc một tỉnh (Ví dụ: An Giang)
    List<District> findByProvinceId(String provinceId);

    // Query PostGIS: Lấy ranh giới huyện dưới dạng GeoJSON
    @Query(value = "SELECT ST_AsGeoJSON(geom_boundary) FROM vn_districts WHERE district_id = :id", nativeQuery = true)
    String getBoundaryAsGeoJson(@Param("id") String id);
}
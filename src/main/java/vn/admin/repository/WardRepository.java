package vn.admin.repository;

import vn.admin.entity.Ward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface WardRepository extends JpaRepository<Ward, String> {
    List<Ward> findByDistrictId(String districtId);

    // Tìm các UBND xã trong bán kính X mét từ một điểm (Dành cho chức năng tìm quanh đây)
    @Query(value = "SELECT * FROM vn_wards WHERE ST_DWithin(ubnd_geom, ST_SetSRID(ST_Point(:lon, :lat), 4326), :distance)", nativeQuery = true)
    List<Ward> findNearbyUbnd(@Param("lon") double lon, @Param("lat") double lat, @Param("distance") double distance);
}
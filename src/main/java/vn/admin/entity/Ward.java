package vn.admin.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "vn_wards")
@Data
public class Ward {
    @Id
    @Column(name = "ward_id")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id")
    private District district;

    @Column(name = "name_vn")
    private String nameVn;

    @Column(name = "area_km2")
    private Double areaKm2;

    // Ranh giới xã
    @Column(name = "geom_boundary", columnDefinition = "geometry(MultiPolygon, 4326)")
    private MultiPolygon geomBoundary;

    // Tọa độ trụ sở UBND (Lấy từ dữ liệu OSM)
    @Column(name = "ubnd_geom", columnDefinition = "geometry(Point, 4326)")
    private Point ubndGeom;

    // Tọa độ tâm xã (Dự phòng)
    @Column(name = "center_geom", columnDefinition = "geometry(Point, 4326)")
    private Point centerGeom;
}
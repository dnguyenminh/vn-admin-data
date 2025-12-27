package vn.admin.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "vn_provinces")
@Data
public class Province {
    @Id
    @Column(name = "province_id")
    private String id;

    @Column(name = "name_vn")
    private String nameVn;

    // Ranh giới tỉnh (vùng)
    @Column(name = "geom_boundary", columnDefinition = "geometry(MultiPolygon, 4326)")
    private MultiPolygon geomBoundary;

    // Điểm đặt tên tỉnh
    @Column(name = "geom_label", columnDefinition = "geometry(Point, 4326)")
    private Point geomLabel;
}
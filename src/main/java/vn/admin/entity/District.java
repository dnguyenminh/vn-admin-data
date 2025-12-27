package vn.admin.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "vn_districts")
@Data
public class District {
    @Id
    @Column(name = "district_id")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_id")
    private Province province;

    @Column(name = "name_vn")
    private String nameVn;

    @Column(name = "geom_boundary", columnDefinition = "geometry(MultiPolygon, 4326)")
    private MultiPolygon geomBoundary;

    @Column(name = "geom_label", columnDefinition = "geometry(Point, 4326)")
    private Point geomLabel;
}
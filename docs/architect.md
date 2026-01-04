# TÀI LIỆU KIẾN TRÚC HỆ THỐNG (ARCHITECTURE DOCUMENT)

**Dự án:** Vietnam GIS Administrative Tool (Java Spring Boot)

## 1. Tổng quan kiến trúc (High-Level Architecture)

Hệ thống được xây dựng theo mô hình **Java Spring Boot Application**, cung cấp các API để truy xuất dữ liệu hành chính và hình học không gian (GIS).

### 1.1. Các thành phần chính:

* **Java Spring Boot:** Backend framework chính.
* **H2 Database / PostGIS:** Lưu trữ dữ liệu hành chính và hình học.
* **JTS (Java Topology Suite):** Xử lý các đối tượng hình học trong Java.
* **Leaflet (Static):** Giao diện bản đồ đơn giản được phục vụ từ thư mục `static/` của backend để kiểm tra nhanh.

---

## 2. Luồng dữ liệu

1. **Import:** Dữ liệu OSM PBF và GADM GeoJSON được xử lý bởi `DataImporter` để nạp vào cơ sở dữ liệu.
2. **API:** `MapController` và `AdminController` cung cấp các endpoints RESTful.
3. **Database Access:** `MapService` sử dụng `JdbcTemplate` và các câu lệnh SQL không gian để truy vấn và trả về GeoJSON trực tiếp cho API.

---

## 3. Cấu trúc mã nguồn chính

```text
src/main/java/vn/admin/
├── config/             # Cấu hình (JtsModule, AppConfig...)
├── controller/         # REST Controllers (MapController, AdminController)
├── entity/             # JPA Entities (nếu dùng JPA cho metadata)
├── repository/         # Spring Data Repositories
├── service/            # Logic nghiệp vụ (MapService, DataImporter)
└── web/                # Lớp ứng dụng Web (MapApplication)
```

---
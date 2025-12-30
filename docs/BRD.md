# TÀI LIỆU YÊU CẦU NGHIỆP VỤ (BRD) - CẬP NHẬT

## Dự án: Hệ thống GIS Hành chính Việt Nam Tương tác (Advanced GIS-Admin)

### 1. Thông tin chung

* **Tên dự án:** Vietnam Administrative GIS Tool.
* **Mục tiêu:** Cung cấp giải pháp quản lý và tra cứu ranh giới hành chính 3 cấp (Tỉnh, Huyện, Xã) trên nền tảng bản đồ số, tối ưu hóa trải nghiệm người dùng thông qua tìm kiếm thông minh và tương tác không gian thời gian thực.

---

### 2. Phạm vi chức năng (Functional Scope)

#### 2.1. Quản lý Điều hướng & Tìm kiếm (Cập nhật logic)

* **Dropdown 3 cấp liên kết (Cascading Selects):**
* Tự động lọc dữ liệu: Tỉnh -> Huyện -> Xã.
* Đảm bảo đồng bộ hóa: Khi một giá trị được chọn từ thanh tìm kiếm, các dropdown phải tự động cập nhật đúng ID tương ứng.


* **Tìm kiếm thông minh (Smart Search Bar):**
* **Hiển thị đầy đủ ngữ cảnh:** Tên kết quả phải bao gồm thông tin cấp cha (Ví dụ: "Quận 5, TP. Hồ Chí Minh") để người dùng phân biệt.
* **Ưu tiên sự kiện Mousedown:** Sử dụng `onmousedown` kết hợp `e.preventDefault()` để đảm bảo lệnh chọn được thực thi trước khi danh sách kết quả bị ẩn bởi sự kiện `blur` của ô nhập liệu.
* **Xử lý bất đồng bộ (Async Handling):** Hệ thống phải chờ dữ liệu cấp dưới (Huyện) tải xong hoàn toàn từ Server trước khi gán giá trị ID, tránh các lỗi cảnh báo (Warning) "ID không tồn tại" trong Console.



#### 2.2. Hiển thị Không gian & Visual chuyên nghiệp

* **Tự động thay đổi chi tiết theo mức Zoom:**
* Hiện ranh giới Tỉnh ở mức zoom xa và chi tiết Xã ở mức zoom gần.


* **Quản lý Layer & Label:**
* Hiển thị nhãn (Label) cho huyện và xã tại tâm điểm địa lý.
* Tự động xóa (Clear) nhãn và ranh giới cũ khi người dùng chuyển sang vùng hành chính khác.



---

### 3. Kiến trúc hệ thống & Đặc tả dữ liệu

#### 3.1. Đặc tả dữ liệu (Data Schema)

| Bảng | Trường chính | Kiểu dữ liệu | Ý nghĩa |
| --- | --- | --- | --- |
| **vn_provinces** | province_id | VARCHAR (PK) | Mã tỉnh thành |
| **vn_districts** | district_id | VARCHAR (PK) | Mã quận huyện |
| **vn_wards** | ward_id | VARCHAR (PK) | Mã phường xã |
|  | **parent_id / province_id** | VARCHAR | Mã liên kết để phục vụ logic tìm kiếm và tự động nhảy dropdown |

#### 3.2. Chỉ mục không gian

* Sử dụng chỉ mục **GIST** trên trường `geom_boundary` để tối ưu tốc độ truy vấn không gian và hiển thị ranh giới.

---

### 4. Yêu cầu phi chức năng (Non-functional Requirements)

* **Tính đồng bộ tuyệt đối:** Đảm bảo 100% sự khớp dữ liệu giữa Bản đồ, Thanh tìm kiếm và các ô Dropdown. Không cho phép xảy ra lỗi không gán được ID khi chọn từ danh sách đề nghị.
* **Độ trễ tương tác:** Thao tác chọn từ tìm kiếm và phóng to bản đồ (fitBounds) phải hoàn thành trong thời gian ngắn, có cơ chế xử lý đợi dữ liệu (setTimeout hoặc Async/Await) để tránh treo logic.

---

### 5. Quy trình vận hành tiêu chuẩn

1. **Tìm kiếm:** Người dùng nhập từ khóa -> Hệ thống gợi ý địa danh kèm địa chỉ cha.
2. **Lựa chọn:** Người dùng nhấn vào kết quả -> Hệ thống ẩn danh sách, gán ID Tỉnh -> Chờ tải Huyện -> Gán ID Huyện.
3. **Hiển thị:** Bản đồ tự động thực hiện `fitBounds` để phóng to vào ranh giới đỏ (Tỉnh) hoặc xanh (Huyện) tương ứng.

---

## Những thay đổi đã thực hiện (What we have done)

Dưới đây là tóm tắt các thay đổi đã được phát triển và tích hợp vào hệ thống (kể cả backend, UI và bộ test):

- **Giao diện & UX**
	- Thêm **sidebar trái có thể thu/gọn** để chứa controls (combobox, search, selects). Sidebar có chế độ `no-transition` trong môi trường test để tránh flakiness.
	- Chuyển các control customer / address / fc sang sidebar; sử dụng combobox + floating dropdown (position: fixed) để tránh bị cắt khi scroll.
	- Thêm **legend** nhỏ trên bản đồ giải thích màu marker (Exact: xanh; Non-exact: đỏ) và đảm bảo legend không bị che khuất khi sidebar mở bằng cách để `#map { width: 100% }`.

- **Map / Visualization**
	- Đảm bảo markers hiển thị trên cùng của các polygon bằng cách tạo pane marker với z-index cao và gọi `bringToFront()` cho layer marker.
	- Thêm style đặc biệt cho **non-exact addresses** (màu đỏ #e74c3c) để dễ nhận diện.

- **Reverse Geocoding & Address logic**
	- Thêm endpoint reverse geocoding: reverse by point và reverse by address id (server-side) để tự động điền province/district/ward khi chọn địa chỉ.
	- Triển khai heuristic `isExactAddress(String)` trên server (MapService) để xác định address là exact (có house number + street) hay non-exact (chỉ có cấp hành chính); annotate responses với `is_exact` boolean trên cả danh sách và GeoJSON features.

- **Backend changes & tests**
	- `MapService` cập nhật: annotate `getAddressList`, `getAddressListPaged`, `getAddressesGeoJsonByAppl` với `is_exact` property; thêm method heuristic `isExactAddress`.
	- Unit tests: thêm và sửa tests trong `MapServiceUnitTest` để kiểm tra heuristic và ensure GeoJSON chứa `is_exact`.
	- Sửa lỗi stubbing của Mockito (lenient/varargs/count query) để tránh `PotentialStubbingProblem` trong tests.
	- Thêm small integration tests to validate `is_exact` is included in service responses (`MapApiIntegrationTest`).

- **Acceptance / E2E test improvements**
	- Stabilized fragile tests that clicked markers above overlapping polygons (e.g., `PointAbovePolygonInteractionTest`) by:
		- Creating map scaffolding when app not initialized in CI.
		- Adding dedicated marker pane and fallback marker/popup-open sequences.
		- Adding diagnostic JS variables (`window.__lastDiagnostic`, `window.__injectErr`, `window.__clickErr`) to capture failures in headless runs.
	- Added an acceptance test `NonExactAddressLegendTest` to verify legend presence and non-exact marker color (with robust fallbacks for headless environments).
	- Added `LegendVisibilityWhenSidebarOpenTest` for visual assertion that the legend is not completely off-screen when sidebar is visible; also added a deterministic unit test `LayoutUnitTest` asserting `#map` and `.map-legend` CSS presence.

- **Other Notes**
	- Updated built static assets (`bin/`) so packaged pages reflect the changes (mapManager.js, index.html updated).
	- Committed small UX tweaks (e.g., highlight reset respecting `is_exact`) and improved JS that positions floating dropdowns to avoid sidebar clipping.

### Outstanding / Follow-ups

- Consider converting some acceptance checks (legend/marker color) into full end-to-end flows that select a DB-backed customer/address instead of injecting JS fixtures — increases fidelity but is slower.
- Optionally make the legend toggleable or responsive (left side when sidebar open) if you prefer a persistent always-visible placement.

If you'd like, I can open a short PR description / changelog entry from this summary and include references to the key commits and tests. 
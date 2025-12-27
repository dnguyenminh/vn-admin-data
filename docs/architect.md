# TÀI LIỆU KIẾN TRÚC HỆ THỐNG (ARCHITECTURE DOCUMENT)

**Dự án:** Vietnam GIS Administrative Tool (Nuxt 3 Framework)

## 1. Tổng quan kiến trúc (High-Level Architecture)

Hệ thống được xây dựng theo mô hình **Isomorphic Web Application**, tận dụng sức mạnh của Nuxt 3 để xử lý dữ liệu hành chính phức tạp một cách reactive.

### 1.1. Các thành phần chính:

* **Nuxt 3 Framework:** Quản lý cấu trúc dự án, Routing và cơ chế Auto-import.
* **Pinia (State Management):** Đóng vai trò là "Bộ não" của hệ thống, quản lý việc đồng bộ giữa Search, Dropdown và Bản đồ.
* **Leaflet.js:** Thư viện hiển thị bản đồ (chạy dưới chế độ Client-only).
* **Composables Layer:** Chứa các logic gọi API và xử lý dữ liệu hành chính dùng chung.

---

## 2. Luồng dữ liệu và Đồng bộ trạng thái (Reactive Data Flow)

Vấn đề lỗi "chọn không được" ở bản thuần cũ sẽ được giải quyết triệt để nhờ luồng dữ liệu một chiều (Unidirectional Data Flow) của Pinia:

1. **Người dùng tương tác:** Click vào một kết quả tìm kiếm (Search) hoặc chọn một Dropdown.
2. **Cập nhật Store (Pinia):** Giá trị `provinceId`, `districtId`, `wardId` được cập nhật vào Store toàn cục.
3. **Phản ứng tự động (Reactivity):**
* **Dropdown Component:** Tự động nhảy đến giá trị mới dựa trên ID trong Store.
* **Map Component:** Tự động "Watch" (theo dõi) ID trong Store để gọi API GeoJSON ranh giới và thực hiện `map.fitBounds()`.



---

## 3. Cấu trúc thư mục chi tiết

```text
/
├── components/           # Các thành phần giao diện nhỏ
│   ├── Map.client.vue    # Component bản đồ Leaflet (Client-side)
│   ├── SearchBar.vue     # Thanh tìm kiếm với logic Mousedown fix lỗi focus
│   └── FilterPanel.vue   # Chứa 3 dropdown Tỉnh, Huyện, Xã
├── composables/          # Chứa logic nghiệp vụ tái sử dụng
│   ├── useGisApi.ts      # Chứa các hàm fetch: getProvinces(), getBounds()...
│   └── useMapUtils.ts    # Các hàm xử lý bản đồ (setStyle, clearLayers)
├── stores/               # Quản lý trạng thái Pinia
│   └── gisStore.ts       # Nơi lưu trữ duy nhất các ID đang chọn
├── layouts/              # Giao diện khung (Default)
└── pages/
    └── index.vue         # Trang chính, lắp ghép các components lại với nhau

```

---

## 4. Đặc tả thành phần kỹ thuật (Technical Specifications)

### 4.1. Pinia Store (`gisStore.ts`)

Store sẽ giữ trạng thái "Sự thật duy nhất" (Single Source of Truth):

* `selectedProvince`: ID tỉnh đang chọn.
* `selectedDistrict`: ID huyện đang chọn.
* `selectedWard`: ID xã đang chọn.
* `Action: setProvince(id)`: Hàm này sẽ tự động reset huyện/xã khi tỉnh thay đổi.

### 4.2. Map Component (`Map.client.vue`)

Do Leaflet cần truy cập vào DOM (Window), component này sẽ được bọc trong `<ClientOnly>` của Nuxt.

* **Watcher:** `watch(selectedProvince, (newVal) => { fetchBounds(newVal) })`.

### 4.3. Search Logic

Tận dụng sự kết hợp giữa Pinia và Vue 3 Composition API:

* Sử dụng sự kiện `mousedown` để ghi đè giá trị vào Store.
* Store thay đổi sẽ tự động đẩy dữ liệu xuống các Dropdown mà không cần `dispatchEvent` thủ công như bản cũ.

---

## 5. Lộ trình triển khai (Deployment Roadmap)

1. **Giai đoạn 1:** Cấu trúc dự án Nuxt 3 và cài đặt Leaflet, Pinia.
2. **Giai đoạn 2:** Chuyển đổi Logic gọi API sang `composables`.
3. **Giai đoạn 3:** Xây dựng `GisStore` để quản lý việc nhảy ID giữa các cấp hành chính.
4. **Giai đoạn 4:** Chuyển đổi phần giao diện CSS sang các Scoped Style trong Component để tránh ghi đè (Conflicts).

---

Tài liệu kiến trúc này đảm bảo hệ thống của bạn sẽ không bao giờ gặp lại lỗi "Warning ID" hay lỗi mất đồng bộ vì mọi thứ đều xoay quanh một Store dữ liệu duy nhất.

**Bước tiếp theo:** Bạn có muốn tôi viết mã mẫu cho file `gisStore.ts` (trái tim của hệ thống) để bạn thấy cách nó điều khiển 3 dropdown không?
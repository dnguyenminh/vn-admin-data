### Functional Specification Document (FSD)
**Project:** Vietnam Administrative GIS Tool
**Version:** 1.0 (Current State Analysis)

---

#### 1. Epic: Map Visualization & Navigation
**Description:** Core map rendering and interaction capabilities using Leaflet.js.

*   **Story 1.1: Full-Screen Map Rendering**
    *   **As a** User,
    *   **I want** to see a full-screen map of Vietnam when I load the page,
    *   **So that** I have a comprehensive view of the geospatial data.
    *   **Acceptance Criteria:**
        *   Map initializes centered on coordinates `[10.5, 105.1]` with zoom level `7`.
        *   OpenStreetMap tiles are loaded and displayed as the base layer.

*   **Story 1.2: Dynamic Layer Styling**
    *   **As a** User,
    *   **I want** administrative boundaries to change color when I hover over them,
    *   **So that** I can easily distinguish the specific area I am interacting with.
    *   **Acceptance Criteria:**
        *   **Mouseover:** The target District polygon highlights in green (`#2ecc71`) with 40% opacity.
        *   **Mouseout:** The polygon reverts to its default transparent red style.
        *   **Selection:** The selected District highlights with higher opacity (70%) and a thicker border.

*   **Story 1.3: Auto-Zoom (FitBounds)**
    *   **As a** User,
    *   **I want** the map to automatically zoom and center on the administrative unit I select,
    *   **So that** I don't have to manually pan and zoom to find it.
    *   **Acceptance Criteria:**
        *   Selecting a **Province** zooms the map to fit the province's bounds.
        *   Selecting a **District** zooms the map to fit that district's geometry.
        *   Clearing a selection resets the view to the parent level (e.g., clearing District zooms back to Province).

*   **Story 1.4: Administrative Labels**
    *   **As a** User,
    *   **I want** to see text labels for Districts and Wards directly on the map,
    *   **So that** I can identify areas without clicking on them.
    *   **Acceptance Criteria:**
        *   District names appear as orange labels at the geometric center of each district.
        *   Ward names appear as smaller blue labels when zooming into a district.

---

#### 2. Epic: Administrative Browsing (Dropdowns)
**Description:** Hierarchical selection of administrative units via the control panel.

*   **Story 2.1: Province Selection**
    *   **As a** User,
    *   **I want** to select a Province from a dropdown list,
    *   **So that** I can filter the map view to a specific province.
    *   **Acceptance Criteria:**
        *   Dropdown is populated with data from `GET /api/map/provinces`.
        *   Selecting a province loads its boundary polygon (`GET /api/map/province/bounds`).
        *   Selecting a province triggers the loading of its child Districts.

*   **Story 2.2: Cascading District Selection**
    *   **As a** User,
    *   **I want** the "District" dropdown to update automatically when I select a Province,
    *   **So that** I only see districts that belong to the selected province.
    *   **Acceptance Criteria:**
        *   The District dropdown is disabled or empty until a Province is selected.
        *   Upon Province selection, the system fetches districts via `GET /api/map/districts?provinceId={id}`.
        *   Selecting a District loads its detailed Ward boundaries (`GET /api/map/wards/geojson`).

*   **Story 2.3: Cascading Ward Selection**
    *   **As a** User,
    *   **I want** the "Ward" dropdown to update automatically when I select a District,
    *   **So that** I can drill down to the lowest administrative level.
    *   **Acceptance Criteria:**
        *   The Ward dropdown is populated via `GET /api/map/wards?districtId={id}` upon District selection.

---

#### 3. Epic: Smart Search
**Description:** Quick lookup functionality with autocomplete and intelligent context handling.

*   **Story 3.1: Autocomplete Search**
    *   **As a** User,
    *   **I want** to see search suggestions as I type in the search bar,
    *   **So that** I can find a location quickly without knowing its exact spelling.
    *   **Acceptance Criteria:**
        *   Search initiates after 2 characters are typed (debounced by 400ms).
        *   Suggestions are fetched from `GET /api/map/search?q={query}`.
        *   Results display the name and type (e.g., "Quận 1 (district)").

*   **Story 3.2: Context-Aware Selection**
    *   **As a** User,
    *   **I want** the system to automatically fill the dropdowns when I select a search result,
    *   **So that** the application state stays synchronized with the map.
    *   **Acceptance Criteria:**
        *   **Scenario:** User searches for and selects a **District**.
        *   **Action:**
            1.  The system identifies the parent Province ID.
            2.  The system automatically selects that Province in the first dropdown.
            3.  The system waits for the District list to load (Async/Await).
            4.  The system automatically selects the target District in the second dropdown.
            5.  The map zooms to the selected District.

---

#### 4. Epic: Technical Performance
**Description:** Backend optimization features observed in the code.

*   **Story 4.1: GeoJSON Lazy Loading**
    *   **As a** Developer/System,
    *   **I want** to load heavy geometry data (GeoJSON) separately from list data,
    *   **So that** the UI remains responsive and dropdowns populate instantly.
    *   **Acceptance Criteria:**
        *   List endpoints (IDs, Names) are separate from GeoJSON endpoints.
        *   Example: `GET /districts` (Metadata) vs `GET /districts/geojson` (Geometry).

*   **Story 4.2: Spatial Indexing**
    *   **As a** User,
    *   **I want** spatial queries to be fast,
    *   **So that** the map renders boundaries without lag.
    *   **Technical Constraint:** Backend uses PostGIS `GIST` indexes on `geom_boundary` columns.
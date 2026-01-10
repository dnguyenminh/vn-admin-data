# Address feature tests ✅

Purpose
- Validate address-related UI behaviors on the map (tooltips, reverse lookup, marker interactions).

Key feature files
- `address_tooltip.feature` — verifies tooltip content for address markers (appl_id, address, location, etc.)
- `address_reverse_lookup.feature` — tests reverse lookup behavior when selecting an address

How to run
- Use Gradle: `./gradlew test` (reporting is handled by Serenity → `build/serenity`).
- To run just address scenarios, use an IDE or filter by tags or package-level runner.

Tips & notes
- These tests interact with the Leaflet map; timing and tile loading can cause flakiness—use `MapManager.waitForMapLayersReady()` or the `the map is ready for interaction` step to stabilize tests.
- If a tooltip does not appear, check for popups opened via coordinates or marker id helpers added to `MapManager` (e.g., `openPopupAtLatLng`).

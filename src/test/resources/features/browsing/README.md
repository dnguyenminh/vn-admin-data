# Browsing feature tests 🔎

Purpose
- Verify administrative browsing and navigation of boundary layers (province/district/ward) in the map UI.

Key feature files
- `administrative_browsing.feature` — tests selecting administrative levels and loading boundary tiles.

How to run
- `./gradlew test` (filter or run the specific feature using your IDE for quicker iteration).

Tips & notes
- Tests assume GeoJSON boundaries are available; confirm backend fixtures or test data if failures occur.
- Use `the map is ready for interaction` precondition to avoid race conditions with tile layers.

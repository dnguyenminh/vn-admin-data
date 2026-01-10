# Legend feature tests 🗺️

Purpose
- Ensure the map legend displays the expected items and descriptions (marker swatches, labels for exact/predicted addresses).

Key feature files
- `legend_features.feature` — tests presence and content of the legend and associated swatches.

How to run
- `./gradlew test` or run the feature directly in the IDE.

Tips & notes
- Legend tests are UI-only and usually stable; they are good quick checks after frontend changes.
- If legend items are moved in the DOM, update selectors in the relevant PageObjects.

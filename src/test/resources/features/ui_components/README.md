# UI Components feature tests 🧩

Purpose
- Validate individual UI components used in the map app (connector labels, checkin popups, sidebars, combobox controls).

Key feature files
- `connector_label.feature` — ensures connector labels render and calculate distances correctly.
- `checkin_popup.feature` — verifies checkin popup content and connector drawing.
- `sidebar_visual.feature` — checks sidebar visibility and control state.

How to run
- `./gradlew test` or run an individual feature in the IDE.

Tips & notes
- Component tests are useful to isolate visual regressions; run them quickly during frontend tweaks.
- If markup changes break selectors, update `PageObject` classes (e.g., `ComboboxControl`) accordingly.
- Use screenshots from Serenity reports to quickly triage UI regressions.

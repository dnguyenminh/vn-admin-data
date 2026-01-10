# Map interaction feature tests 🛰️

Purpose
- Validate interactive behaviors of the map (focus controls, checkins, predictions, keyboard navigation, and layer interactions).

Key feature files
- `focus_controls.feature` — tests focusing map on selected FC/address and control flows.
- `map_layers_interaction.feature` — verifies interactions between layers (districts, wards, provinces).
- `fc_focus_prediction.feature` — tests field collector focus & prediction flows.
- `checkins_and_controls.feature` — tests checkin visibility and control state transitions.
- `keyboard_navigation.feature` — keyboard shortcuts and accessibility navigation.
- `show_predicted_race_condition.feature` — regression for predicted address race conditions.

How to run
- `./gradlew test` (run specific features in the IDE to speed up iteration).

Tips & notes
- These tests are the most timing-sensitive; ensure `MapManager.waitForMapLayersReady()` is used where appropriate.
- When flakiness arises in combo/select interactions, review `SelectFromCombobox` and use screenshot evidence in `build/serenity/screenshots` to diagnose.
- If tests need deterministic markers, use `InjectSyntheticAddress` test helper to seed predictable features.

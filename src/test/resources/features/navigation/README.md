# Navigation feature tests 🧭

Purpose
- Ensure the general application navigation and map visualization flows are correct (page transitions, map centering, route to map).

Key feature files
- `map_visualization.feature` — validates high-level map presentation and navigation behavior.

How to run
- `./gradlew test` or execute the single feature in your IDE for faster checks.

Tips & notes
- Navigation tests often rely on the app's startup and profile configuration; use the `dev` profile for local runs (`./gradlew bootRunDev`).
- Keep scenarios focused on navigation to reduce flakiness tied to map rendering.

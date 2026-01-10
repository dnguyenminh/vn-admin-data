# Layout feature tests 🧭

Purpose
- Verify static layout and UI composition (sidebar, legend, map container layout and accessibility).

Key feature files
- `layout_verification.feature` — checks page structure, visibility of controls, and basic layout constraints.

How to run
- `./gradlew test` or run the single scenario via IDE for faster feedback.

Tips & notes
- Layout tests are generally deterministic; failures often indicate CSS changes or viewport differences.
- If running headless, ensure browser window size and retina scaling are consistent to avoid pixel-related failures.

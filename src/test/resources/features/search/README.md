# Search feature tests 🔍

Purpose
- Validate map search capabilities and search-driven interactions (e.g., search results centering, filter suggestions).

Key feature files
- `map_search.feature` — tests search box behavior and result selection.

How to run
- `./gradlew test` or run the search feature from an IDE.

Tips & notes
- Search tests depend on backend fixtures (search indexing); ensure test data is present or mocked.
- If typeahead behavior is flaky, consider increasing wait times or using direct JS hooks for deterministic selection in tests.

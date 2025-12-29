# Next implementation steps (concrete, prioritized)

This file lists small PR-sized tasks I will implement next to finish the remaining work.

1) Return typed JSON from MapService
- Change MapService geojson methods to return Jackson JsonNode (or Map) instead of raw String.
- Let MapController keep using ObjectMapper to return ResponseEntity<JsonNode>.
- Add unit tests that call MapService methods with an in-memory JdbcTemplate (or Testcontainers) to validate output shape.

2) Add integration tests for search and bounds
- Extend existing Testcontainers test to assert /api/map/search and /api/map/province/bounds responses.
- Add a small test dataset seed (1 province/district/ward) and assert expected behavior.

3) Harden DataImporter further (follow-up)
- Add transactional batching option and a CLI runner that supports `--dryRun`.
- Document backup commands in docs/PROPOSAL.md (already present).

4) CI / local dev guidance
- Add a README snippet for running tests with Docker available:
  - ./gradlew test --tests vn.admin.MapApiIntegrationTest
- Document migration script usage: scripts/migrations/001_add_search_spatial_indexes.sql

5) Optional improvements (low effort)
- Add logging around SQL exceptions in MapService and map endpoints.
- Add simple metrics (timings) for heavy SQL endpoints (districts/wards geojson).

No blocking questions remain; I will implement item (1) next (MapService → JsonNode) and update the repository.

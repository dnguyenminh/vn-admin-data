Feature: Layout Verification
  As a User
  I want the map layout to be correct
  So that the interface remains usable and visual elements are properly displayed

  Scenario: Map container uses percent width for flex layout
    Then the index.html should contain "#map"
    And the index.html should contain "width: 100%"

  Scenario: Map legend CSS classes exist
    Then the index.html should contain ".map-legend"
    And the index.html should contain ".map-legend .legend-item"

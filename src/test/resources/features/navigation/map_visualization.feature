Feature: Map Visualization and Navigation
  As a User
  I want to interact with the map of Vietnam
  So that I can explore administrative boundaries

  Scenario: Full-Screen Map Rendering
    Given the user is on the map page
    Then the map should be displayed in full screen
    And the map center should be approximately 10.5 latitude and 105.1 longitude

  Scenario: Dynamic Layer Styling on Mouse Interaction
    Given the user is on the map page
    When the user hovers over a district
    Then the district boundary should be highlighted in green
    When the user moves the mouse away from the district
    Then the district boundary should revert to its default style

  Scenario: Auto-Zoom to Selected Province
    Given the user is on the map page
    When the user selects "Hà Nội" from the province dropdown
    Then the map should zoom to fit the bounds of "Hà Nội"

  Scenario: Display Administrative Labels
    Given the user is on the map page
    And "Hà Nội" is selected as the province
    Then the map should display labels for districts

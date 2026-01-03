Feature: Map Layers and Interaction
  As a User
  I want to interact with map layers correctly
  So that I can select features even when they overlap

  Scenario: Clicking point on top of polygons opens popup
    Given the user is on the map page
    And the map and app scaffolding are ready
    When overlapping polygons and a clickable point are injected
    And the user clicks the point marker
    Then a leaflet popup should be shown

  Scenario: Predicted marker should be on top of polygon
    Given the user is on the map page
    And the map is ready for interaction
    When a predicted marker and a polygon are injected
    Then the predicted marker should have a higher z-index than the polygon

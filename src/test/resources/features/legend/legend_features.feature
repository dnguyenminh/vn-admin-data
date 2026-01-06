@focus
Feature: Legend Features
  As a User
  I want to see legends for map features
  So that I can understand what the markers and colors represent

  Scenario: Legend shows address and FC prediction icons
    Given the user is on the map page
    When a legend with prediction icons is injected
    Then the legend should mention "Predicted address"
    And the legend should mention "Predicted FC location"
    And the legend should include the predicted address swatch
    And the legend should include the predicted FC location swatch

  Scenario: Legend stays within viewport when sidebar is open
    Given the user is on the map page
    When a visible sidebar and a legend are simulated
    Then the map width should be adjusted to fit the available viewport

  Scenario: Non-exact address shows special color and legend is present
    Given the user is on the map page
    And the map is ready for interaction
    When a non-exact address marker is injected
    Then the map legend should be present
    And the marker should have the special non-exact color

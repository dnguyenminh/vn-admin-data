Feature: Focus Controls
  As a User
  I want to use focus controls
  So that I can quickly center the map on specific features

  Scenario: Focus buttons exist and focus map
    Given the user is on the map page
    Then the focus controls should be present
    When the user clicks the focus address button
    And the user clicks the focus FC button

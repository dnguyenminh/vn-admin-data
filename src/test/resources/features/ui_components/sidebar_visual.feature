Feature: Sidebar Visuals
  As a User
  I want the sidebar to be visually correct
  So that I have a consistent and usable interface

  Scenario: Sidebar uses full height and toggle does not overlap zoom
    Given the user is on the map page
    And the sidebar is open
    Then the sidebar should use the full viewport height
    And the sidebar toggle should not overlap the Leaflet zoom control

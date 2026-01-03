Feature: Checkin Popup
  As a User
  I want to see detailed information in the checkin popup
  So that I can verify the checkin location and administrative details

  Scenario: Checkin popup includes distance and admin info
    Given the user is on the map page
    And the map is ready for interaction
    When a checkin feature is injected with administrative info
    And the user opens the checkin popup
    Then the popup should display a numeric distance in meters
    And the popup should display province name "Bac Lieu"

Feature: Search for Administrative Units
  As a User
  I want to search for provinces, districts, and wards
  So that I can quickly navigate to specific locations on the map

  Scenario: Search for a Province and auto-select it
    Given the user is on the map page
    When the user searches for "Hà Nội"
    Then the search input should contain "Hà Nội"
    And the province dropdown should display "Hà Nội"

  Scenario: Map interface elements are visible
    Given the user is on the map page
    Then the map should be visible
    And the province dropdown should be visible

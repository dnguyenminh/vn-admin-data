Feature: Address Reverse Lookup
  As a User
  I want to select an address from results
  So that the province, district, and ward selections are automatically populated

  Scenario: Selecting an address populates province, district and ward
    Given the user is on the map page
    And the map is ready for interaction
    When the user selects an address result "Fake Address" with ID "ADDR-1"
    Then the province select should be "P-1"
    And the district select should be "D-1"
    And the ward select should be "W-1"

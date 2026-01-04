Feature: Address Reverse Lookup
  As a User
  I want to select an address from results
  So that the province, district, and ward selections are automatically populated

  Scenario: Selecting an address populates province, district and ward
    Given the user is on the map page
    And the map is ready for interaction
    When the user selects the appl_id "20240601-0919569", the addess "đại trung Xã Đại Đồng Tiên Du Tỉnh Bắc Ninh" and the field collector "FC002533" to the map
    Then the province select should be "Bắc Ninh"
    And the district select should be "Tiên Du"
    And the ward select should be "Đại Đồng"

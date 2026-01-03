Feature: Administrative Browsing via Dropdowns
  As a User
  I want to select administrative units using hierarchical dropdowns
  So that I can drill down from Province to District to Ward


  Scenario Outline: Select a Province from Dropdown (Outline)
    Given the user is on the map page
    When the user selects "<Province>" from the province dropdown
    Then the district dropdown should be populated with districts of "<Province>"
    And the map should zoom to fit the bounds of "<Province>"
    
    Examples:
      | Province  | District | Ward |
      | Bắc Giang |          |      |
      | Hà Nội    |          |      |

  Scenario: Select a Province from Dropdown (Single)
    Given the user is on the map page
    When the user selects "Hà Nội" from the province dropdown
    Then the district dropdown should be populated with districts of "Hà Nội"
    And the map should zoom to fit the bounds of "Hà Nội"

  Scenario: Cascading District Selection
    Given the user is on the map page
    And the user has selected "Hà Nội" as the province
    When the user selects "Quận Ba Đình" from the district dropdown
    Then the ward dropdown should be populated with wards of "Quận Ba Đình"
    And the map should display the ward boundaries for "Quận Ba Đình"

  Scenario: Cascading Ward Selection
    Given the user is on the map page
    And the user has selected "Hà Nội" as the province
    And the user has selected "Quận Ba Đình" as the district
    When the user selects "Phường Phúc Xá" from the ward dropdown
    Then the map should focus on "Phường Phúc Xá"

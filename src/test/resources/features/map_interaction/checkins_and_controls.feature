Feature: Map controls and checkin visibility
  In order to avoid stale controls and to let users inspect checkins easily
  As a user of the map
  I want customer/address/fc controls to be cleared appropriately and the "Show all checkins" toggle to show the expected checkins

  Background:
    Given the user is on the map page
    And the sidebar is visible
    And the map is ready for interaction

  Scenario: Changing customer clears address and FC controls
    When the user selects the appl_id "20240601-0919569" and the address "đại trung Xã Đại Đồng Tiên Du Tỉnh Bắc Ninh" to the map
    And the user selects the appl_id "20240601-0917275" to the map
    Then the address control should contain ""
    And the fc control should contain ""

  Scenario: Changing address clears FC control
    When the user selects the appl_id "00-87-91-81" and the address "THÔN 2 CƯ SUÊ, Huyện Cư M'gar, Tỉnh Đắk Lắk" to the map
    And the user selects the address "CƯ SUÊ, , Tỉnh Đắk Lắk" to the map
    Then the fc control should contain ""

  Scenario: ShowAllCheckins shows checkins for FCs who checked at selected address
    When the user selects the appl_id "20240601-0919569" and the address "đại trung Xã Đại Đồng Tiên Du Tỉnh Bắc Ninh" to the map
    And the user records the visible checkin count
    When the user checks the showAllCheckins checkbox
    Then the visible checkin count should be greater or equal to the recorded count

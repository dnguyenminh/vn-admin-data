Feature: Address Tooltip
  As a User
  I want to see detailed address information in a tooltip
  So that I can identify specific address properties like application ID

  Scenario Outline: Address tooltip displays application ID
    Given the user is on the map page
    And the map is ready for interaction
    When the user selects the appl_id "<appl_id>", the addess "<address>" and the field collector "<fc_id>" to the map

    # Ensure a marker is present for the selected address (inject synthetic if backend did not return it)
    And a synthetic address "<address>" at location (<lat>, <long>) is injected to the map

#    When a customer with appl_id "<appl_id>" is added to the map
#    And an address is "<address>" is added to the map
#    And a field collector is "<fc_id>" is added to the map
    And the user clicks on the address marker
    Then the tooltip for the address should display:
"""
appl_id: <appl_id>
address: <address>
address_type: <address_type>
location(lat, long): (<lat>, <long>)
"""

    Examples:
      | appl_id          | address                                              | fc_id    | address_type | lat       | long       |
      | 20250601-0931300 | Thôn Dêm Phổ Xã Tam Anh Nam Núi Thành Tỉnh Quảng Nam | LFC40794 | TMPADD       | 15.477313 | 108.587318 |

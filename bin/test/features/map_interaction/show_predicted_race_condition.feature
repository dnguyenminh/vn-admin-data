Feature: Prevent race condition where Show Predicted button flips state incorrectly

  @race
  Scenario: Show Predicted button remains disabled when address exactness is preserved across address layer reload
    Given the user is on the map page
    And the map and app scaffolding are ready
    When the user selects the appl_id "20240601-0919569", the addess "đại trung Xã Đại Đồng Tiên Du Tỉnh Bắc Ninh" and the field collector "FC001691" to the map
    When I invoke updateShowFcPredEnabled
    Then the show predicted button should be disabled
    And I invoke updateShowFcPredEnabled
    Then the show predicted button should remain disabled

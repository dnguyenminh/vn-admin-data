Feature: Prevent race condition where Show Predicted button flips state incorrectly

  @race
  Scenario: Show Predicted button remains disabled when address exactness is preserved across address layer reload
    And the map and app scaffolding are ready
    And the selected address "ADDR-1" is known exact in the map
    When I invoke updateShowFcPredEnabled
    Then the show predicted button should be disabled
    When the address layer is reloaded without an explicit is_exact for "ADDR-1"
    And I invoke updateShowFcPredEnabled
    Then the show predicted button should remain disabled

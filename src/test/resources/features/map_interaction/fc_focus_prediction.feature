Feature: FC Focus and Prediction
  As a User
  I want to focus on FC checkins and see prediction markers
  So that I can analyze the spatial distribution of checkins and predictions for a Field Collector

  Scenario: Focus button shows checkins and prediction button shows predicted marker
    Given the user is on the map page
    And the sidebar is visible
    And the map is ready for interaction
    When multiple checkins for FC "FC_TEST" are injected
    And the user selects FC "FC_TEST" from the combobox
    Then no predicted marker should be displayed initially
    When the user triggers the predicted-for-FC action
    Then the predicted marker should be displayed on the map

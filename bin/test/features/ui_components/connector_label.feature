Feature: Connector Label
  As a User
  I want to see the distance label on the connector line
  So that I can know the distance between the stored address and the predicted location

  Scenario: Connector label is displayed with distance
    Given the user is on the map page
    And the map is ready for interaction
    When a predicted address feature is injected with a connector
    Then the connector label should display a numeric distance in meters

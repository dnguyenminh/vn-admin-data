Feature: Keyboard Navigation
  As a User
  I want to navigate through search results using the keyboard
  So that I can quickly select items without using the mouse

  Scenario: Arrow keys move focus and focused item is visible
    Given the user is on the map page
    And a list of deterministic customers is injected
    And the user focuses the customer combobox
    When the user moves focus down to the first item
    Then the first focused customer should be visible
    When the user moves focus down to the third item
    Then the third focused customer should be visible
    When the user moves focus up to the second item
    Then the second focused customer should be visible

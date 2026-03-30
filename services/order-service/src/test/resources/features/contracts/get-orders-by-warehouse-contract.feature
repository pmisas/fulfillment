@contract
Feature: get orders by warehouse contract

  Background:
    * def operatorAToken = tokenOperatorA
    * def orderResponseContract =
    """
    {
      orderId: '#string',
      status: '#string'
    }
    """

  Scenario: returns 200 with a list of OrderResponse
    Given url baseUrl
    And path '/api/v1/orders/by-warehouse', 'wh-1'
    And header Authorization = 'Bearer ' + operatorAToken
    When method get
    Then status 200
    And match response == '#[]'
    And match each response contains orderResponseContract

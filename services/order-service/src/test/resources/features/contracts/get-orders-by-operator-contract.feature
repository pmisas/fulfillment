@contract
Feature: get orders by operator contract

  Background:
    * def operatorAToken = tokenOperatorA
    * def adminToken = tokenAdmin
    * def orderResponseContract =
    """
    {
      orderId: '#string',
      status: '#string'
    }
    """
    * def accessDeniedContract =
    """
    {
      status: 403,
      error: 'ACCESS_DENIED',
      message: '#string',
      fields: null
    }
    """

  Scenario: returns 200 with a list of OrderResponse for admins
    Given url baseUrl
    And path '/api/v1/orders/by-operator', 'operator-a'
    And header Authorization = 'Bearer ' + adminToken
    When method get
    Then status 200
    And match response == '#[]'
    And match each response contains orderResponseContract

  Scenario: returns 403 with ApiErrorResponse for non-admin users
    Given url baseUrl
    And path '/api/v1/orders/by-operator', 'operator-a'
    And header Authorization = 'Bearer ' + operatorAToken
    When method get
    Then status 403
    And match response == accessDeniedContract

@contract
Feature: get orders by status contract

  Background:
    * def operatorAToken = tokenOperatorA
    * def createPayload =
    """
    {
      "lat": 4.7110,
      "lng": -74.0721,
      "items": [
        { "sku": "SKU-1", "quantity": 2 }
      ]
    }
    """
    * def orderResponseContract =
    """
    {
      orderId: '#string',
      status: '#string'
    }
    """
    * def invalidParameterContract =
    """
    {
      status: 400,
      error: 'INVALID_PARAMETER',
      message: '#string',
      fields: null
    }
    """

  Scenario: returns 200 with a list of OrderResponse
    * def idemKey = 'idem-' + java.util.UUID.randomUUID()

    Given url baseUrl
    And path '/api/v1/orders'
    And header Authorization = 'Bearer ' + operatorAToken
    And header Idempotency-Key = idemKey
    And request createPayload
    When method post
    Then status 201

    Given url baseUrl
    And path '/api/v1/orders/by-status/RECEIVED'
    And header Authorization = 'Bearer ' + operatorAToken
    When method get
    Then status 200
    And match response == '#[]'
    And match each response contains orderResponseContract

  Scenario: returns 400 with ApiErrorResponse when the status is invalid
    Given url baseUrl
    And path '/api/v1/orders/by-status/NOT_A_STATUS'
    And header Authorization = 'Bearer ' + operatorAToken
    When method get
    Then status 400
    And match response == invalidParameterContract

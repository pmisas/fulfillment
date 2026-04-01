@contract
Feature: get order by id contract

  Background:
    * def operatorAToken = tokenOperatorA
    * def operatorBToken = tokenOperatorB

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

    * def notFoundContract =
    """
    {
      status: 404,
      error: 'ORDER_NOT_FOUND',
      message: '#string',
      fields: null
    }
    """

    * def accessDeniedContract =
    """
    {
      status: 403,
      error: 'ORDER_ACCESS_DENIED',
      message: '#string',
      fields: null
    }
    """

  Scenario: returns 200 with OrderResponse when the order exists
    * def idemKey = 'idem-' + java.util.UUID.randomUUID()

    Given url baseUrl
    And path '/api/v1/orders'
    And header Authorization = 'Bearer ' + operatorAToken
    And header Idempotency-Key = idemKey
    And request createPayload
    When method post
    Then status 201
    And match response.orderId == '#string'
    And match response.status == '#string'
    * def createdOrderId = response.orderId

    Given url baseUrl
    And path '/api/v1/orders', createdOrderId
    And header Authorization = 'Bearer ' + operatorAToken
    When method get
    Then status 200
    And match response == orderResponseContract
    And match response.orderId == createdOrderId

  Scenario: returns 403 with ApiErrorResponse when another operator tries to access the order
    * def idemKey = 'idem-' + java.util.UUID.randomUUID()

    Given url baseUrl
    And path '/api/v1/orders'
    And header Authorization = 'Bearer ' + operatorAToken
    And header Idempotency-Key = idemKey
    And request createPayload
    When method post
    Then status 201
    And match response.orderId == '#string'
    * def createdOrderId = response.orderId

    Given url baseUrl
    And path '/api/v1/orders', createdOrderId
    And header Authorization = 'Bearer ' + operatorBToken
    When method get
    Then status 403
    And match response == accessDeniedContract

  Scenario: returns 404 with ApiErrorResponse when the order does not exist
    Given url baseUrl
    And path '/api/v1/orders', 'non-existent-id-00000000'
    And header Authorization = 'Bearer ' + operatorAToken
    When method get
    Then status 404
    And match response == notFoundContract
@contract
Feature: get my orders contract

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
    * def badRequestContract =
    """
    {
      status: 400,
      error: 'BAD_REQUEST',
      message: 'Cannot filter by both status and warehouseId simultaneously',
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
    And match response.orderId == '#string'
    And match response.status == '#string'
    * def createdOrderId = response.orderId

    Given url baseUrl
    And path '/api/v1/orders/mine'
    And header Authorization = 'Bearer ' + operatorAToken
    When method get
    Then status 200
    And match response == '#[]'
    And match each response == orderResponseContract
    And match response[*].orderId contains createdOrderId

  Scenario: returns 400 with ApiErrorResponse when status and warehouseId are sent together
    Given url baseUrl
    And path '/api/v1/orders/mine'
    And header Authorization = 'Bearer ' + operatorAToken
    And param status = 'RECEIVED'
    And param warehouseId = 'wh-1'
    When method get
    Then status 400
    And match response == badRequestContract
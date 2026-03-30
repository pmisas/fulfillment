@integration
Feature: order idempotency integration

  Background:
    * url baseUrl
    * def operatorToken = operatorAToken

    * def createOrderPayload =
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

    * def apiErrorResponseContract =
    """
    {
      error: '#string',
      message: '#string'
    }
    """

  Scenario: same idempotency key cannot create a second order while request conflicts
    * def idemKey = 'idem-fixed-' + java.util.UUID.randomUUID()

    Given path '/api/v1/orders'
    And header Authorization = 'Bearer ' + operatorToken
    And header Idempotency-Key = idemKey
    And request createOrderPayload
    When method post
    Then status 201
    And match response == orderResponseContract
    * def firstOrderId = response.orderId

    Given path '/api/v1/orders'
    And header Authorization = 'Bearer ' + operatorToken
    And header Idempotency-Key = idemKey
    And request createOrderPayload
    When method post
    Then status 409
    And match response == apiErrorResponseContract
    * match response.error == '#string'
    * match response.message == '#string'
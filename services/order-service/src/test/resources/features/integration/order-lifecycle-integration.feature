@integration
Feature: order lifecycle integration

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

    * def asyncCancellationResponseContract =
    """
    {
      orderId: '#string',
      message: '#string',
      status: 'PROCESSING'
    }
    """

  Scenario: create an order, get it by id and request cancellation successfully
    * def idemKey = 'idem-' + java.util.UUID.randomUUID()

    Given path '/api/v1/orders'
    And header Authorization = 'Bearer ' + operatorToken
    And header Idempotency-Key = idemKey
    And request createOrderPayload
    When method post
    Then status 201
    And match response == orderResponseContract
    And match response.status == 'RECEIVED'
    * def createdOrderId = response.orderId

    Given path '/api/v1/orders', createdOrderId
    And header Authorization = 'Bearer ' + operatorToken
    When method get
    Then status 200
    And match response == orderResponseContract
    And match response.orderId == createdOrderId
    And match response.status == 'RECEIVED'

    Given path '/api/v1/orders', createdOrderId, 'cancel'
    And header Authorization = 'Bearer ' + operatorToken
    When method post
    Then status 202
    And match response == asyncCancellationResponseContract
    And match response.orderId == createdOrderId
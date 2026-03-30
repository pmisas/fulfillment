@contract
Feature: cancel order contract

  Background:
    * def operatorAToken = tokenOperatorA
    * def operatorBToken = tokenOperatorB
    * def createOrderRequestContract =
    """
    {
      lat: '#number',
      lng: '#number',
      items: '#[] ##object'
    }
    """
    * def orderItemContract =
    """
    {
      sku: '#string',
      quantity: '#number'
    }
    """
    * def cancelOrderResponseContract =
    """
    {
      orderId: '#string',
      message: 'Order cancellation has been requested and is being processed. The order will be cancelled shortly and inventory will be released.',
      status: 'PROCESSING'
    }
    """
    * def orderNotFoundContract =
    """
    {
      status: 404,
      error: 'ORDER_NOT_FOUND',
      message: '#string',
      fields: null
    }
    """
    * def orderAccessDeniedContract =
    """
    {
      status: 403,
      error: 'ORDER_ACCESS_DENIED',
      message: '#string',
      fields: null
    }
    """
    * def invalidStatusTransitionContract =
    """
    {
      status: 400,
      error: 'INVALID_STATUS_TRANSITION',
      message: '#string',
      fields: null
    }
    """

  Scenario: cancel order returns the published success contract
    * def idemKey = 'idem-' + java.util.UUID.randomUUID()
    * def payload =
    """
    {
      "lat": 4.7110,
      "lng": -74.0721,
      "items": [
        { "sku": "SKU-1", "quantity": 2 }
      ]
    }
    """
    * match payload == createOrderRequestContract
    * match each payload.items == orderItemContract

    Given url baseUrl
    And path '/api/v1/orders'
    And header Authorization = 'Bearer ' + operatorAToken
    And header Idempotency-Key = idemKey
    And request payload
    When method post
    Then status 201
    * def createdOrderId = response.orderId

    Given url baseUrl
    And path '/api/v1/orders', createdOrderId, 'cancel'
    And header Authorization = 'Bearer ' + operatorAToken
    When method post
    Then status 202
    And match response == cancelOrderResponseContract
    And match response.orderId == createdOrderId

  Scenario: cancel non-existent order returns the published 404 error contract
    Given url baseUrl
    And path '/api/v1/orders', 'non-existent-id-00000000', 'cancel'
    And header Authorization = 'Bearer ' + operatorAToken
    When method post
    Then status 404
    And match response == orderNotFoundContract
 
  Scenario: cancel order owned by another operator returns the published 403 error contract
    * def idemKey = 'idem-' + java.util.UUID.randomUUID()
    * def payload =
    """
    {
      "lat": 4.7110,
      "lng": -74.0721,
      "items": [
        { "sku": "SKU-1", "quantity": 2 }
      ]
    }
    """
    * match payload == createOrderRequestContract
    * match each payload.items == orderItemContract

    Given url baseUrl
    And path '/api/v1/orders'
    And header Authorization = 'Bearer ' + operatorAToken
    And header Idempotency-Key = idemKey
    And request payload
    When method post
    Then status 201
    * def createdOrderId = response.orderId

    Given url baseUrl
    And path '/api/v1/orders', createdOrderId, 'cancel'
    And header Authorization = 'Bearer ' + operatorBToken
    When method post
    Then status 403
    And match response == orderAccessDeniedContract

  //TODO probar  cancelar orden en estado no cancelable


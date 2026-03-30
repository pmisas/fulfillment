@contract
Feature: create order contract

  Background:
    * def operatorAToken = tokenOperatorA
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
    * def orderResponseContract =
    """
    {
      orderId: '#string',
      status: '#string'
    }
    """
    * def validationErrorContract =
    """
    {
      status: 400,
      error: 'VALIDATION_ERROR',
      message: '#string',
      fields: '#[] ##object'
    }
    """
    * def conflictErrorContract =
    """
    {
      status: 409,
      error: '#string',
      message: '#string',
      fields: null
    }
    """

  Scenario: returns 201 with OrderResponse when the order is created successfully
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
    And match response == orderResponseContract

  Scenario: returns 400 with ApiErrorResponse when the request payload is invalid
    * def idemKey = 'idem-' + java.util.UUID.randomUUID()
    * def payload =
    """
    {
      "lat": 4.7110,
      "lng": -74.0721,
      "items": [
        { "sku": "SKU-1", "quantity": 0 }
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
    Then status 400
    And match response == validationErrorContract

  Scenario: returns 409 with ApiErrorResponse when the idempotency key conflicts
    * def idemKey = 'idem-fixed-key'
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

    Given url baseUrl
    And path '/api/v1/orders'
    And header Authorization = 'Bearer ' + operatorAToken
    And header Idempotency-Key = idemKey
    And request payload
    When method post
    Then status 201

    Given url baseUrl
    And path '/api/v1/orders'
    And header Authorization = 'Bearer ' + operatorAToken
    And header Idempotency-Key = idemKey
    And request payload
    When method post
    Then status 409
    And match response == conflictErrorContract

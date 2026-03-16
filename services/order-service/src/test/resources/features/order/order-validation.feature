Feature: order validation

  Scenario: create order with blank sku and zero quantity
    * def idemKey = 'idem-' + java.util.UUID.randomUUID()

    Given url baseUrl
    And path '/api/v1/orders'
    And header Idempotency-Key = idemKey
    And request
    """
    {
      "lat": 4.7110,
      "lng": -74.0721,
      "items": [
        { "sku": "", "quantity": 0 }
      ]
    }
    """
    When method post
    Then status 400
    And match response.error == 'VALIDATION_ERROR'
    And match response.fields == '#[]'

  Scenario: create order with empty items list
    * def idemKey = 'idem-' + java.util.UUID.randomUUID()

    Given url baseUrl
    And path '/api/v1/orders'
    And header Idempotency-Key = idemKey
    And request
    """
    {
      "lat": 4.7110,
      "lng": -74.0721,
      "items": []
    }
    """
    When method post
    Then status 400
    And match response.error == 'VALIDATION_ERROR'

  Scenario: create order without lat and lng
    * def idemKey = 'idem-' + java.util.UUID.randomUUID()

    Given url baseUrl
    And path '/api/v1/orders'
    And header Idempotency-Key = idemKey
    And request
    """
    {
      "items": [
        { "sku": "SKU-1", "quantity": 1 }
      ]
    }
    """
    When method post
    Then status 400
    And match response.error == 'VALIDATION_ERROR'
Feature: create order

  Scenario: create a new order
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
        { "sku": "SKU-1", "quantity": 2 }
      ]
    }
    """
    When method post
    Then status 201
    And match response ==
    """
    {
      orderId: '#string',
      status: 'RECEIVED'
    }
    """
Feature: get order by id

  Scenario: create and then get order
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
    * def createdOrderId = response.orderId

    Given url baseUrl
    And path '/api/v1/orders', createdOrderId
    When method get
    Then status 200
    And match response ==
    """
    {
      orderId: '#(createdOrderId)',
      status: 'RECEIVED'
    }
    """

  Scenario: get order with non-existent id returns 404
    Given url baseUrl
    And path '/api/v1/orders', 'non-existent-id-00000000'
    When method get
    Then status 404
    And match response.error == 'ORDER_NOT_FOUND'

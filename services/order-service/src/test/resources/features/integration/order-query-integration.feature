@integration
Feature: order query and authorization integration

  Background:
    * url baseUrl
    * def operatorA = operatorAToken
    * def operatorB = operatorBToken
    * def admin = adminToken

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

    * def orderListContract = '#[] ##object'

  Scenario: mine returns the orders of the authenticated operator
    * def idemKey1 = 'idem-' + java.util.UUID.randomUUID()
    * def idemKey2 = 'idem-' + java.util.UUID.randomUUID()

    Given path '/api/v1/orders'
    And header Authorization = 'Bearer ' + operatorA
    And header Idempotency-Key = idemKey1
    And request createOrderPayload
    When method post
    Then status 201
    * def orderAId = response.orderId

    Given path '/api/v1/orders'
    And header Authorization = 'Bearer ' + operatorB
    And header Idempotency-Key = idemKey2
    And request createOrderPayload
    When method post
    Then status 201
    * def orderBId = response.orderId

    Given path '/api/v1/orders/mine'
    And header Authorization = 'Bearer ' + operatorA
    When method get
    Then status 200
    And match response == orderListContract
    And match each response contains { orderId: '#string', status: '#string' }
    And match response[*].orderId contains orderAId
    And match response[*].orderId !contains orderBId

  Scenario: admin can query orders by operator
    * def idemKey = 'idem-' + java.util.UUID.randomUUID()

    Given path '/api/v1/orders'
    And header Authorization = 'Bearer ' + operatorA
    And header Idempotency-Key = idemKey
    And request createOrderPayload
    When method post
    Then status 201
    * def createdOrderId = response.orderId

    # Ajusta este valor si tu auth real usa otro identificador de operador
    * def operatorAId = 'operator-a'

    Given path '/api/v1/orders/by-operator', operatorAId
    And header Authorization = 'Bearer ' + admin
    When method get
    Then status 200
    And match response == orderListContract
    And match each response contains { orderId: '#string', status: '#string' }
    And match response[*].orderId contains createdOrderId

  Scenario: non admin cannot query orders by operator
    * def operatorAId = 'operator-a'

    Given path '/api/v1/orders/by-operator', operatorAId
    And header Authorization = 'Bearer ' + operatorB
    When method get
    Then status 403
    And match response == apiErrorResponseContract

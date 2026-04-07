@integration
Feature: inventory access integration

  Background:
    * url baseUrl

    * def createWarehousePayload =
    """
    {
      "city": "Medellin",
      "lat": 6.2442,
      "lng": -75.5812
    }
    """

    * def assignManagerPayload =
    """
    {
      "userId": "warehouse-manager-1"
    }
    """

    * def restockPayload =
    """
    {
      "items": [
        { "sku": "SKU-1", "quantity": 10 }
      ]
    }
    """

    * def apiErrorResponseContract =
    """
    {
      status: '#number',
      error: '#string',
      message: '#string',
      fields: '##[] ##object'
    }
    """

  Scenario: user without warehouse access cannot restock inventory
    Given path '/api/v1/warehouses'
    And headers adminHeaders
    And request createWarehousePayload
    When method post
    Then status 201
    And match response.warehouseId == '#string'
    * def warehouseId = response.warehouseId

    Given path '/api/v1/warehouses', warehouseId, 'managers'
    And headers adminHeaders
    And request assignManagerPayload
    When method post
    Then status 201

    Given path '/api/v1/warehouses', warehouseId, 'inventory', 'restock'
    And headers otherManagerHeaders
    And request restockPayload
    When method post
    Then status 403
    And match response == apiErrorResponseContract
    And match response.error == 'FORBIDDEN'

  Scenario: user without warehouse access cannot query inventory
    Given path '/api/v1/warehouses'
    And headers adminHeaders
    And request createWarehousePayload
    When method post
    Then status 201
    And match response.warehouseId == '#string'
    * def warehouseId = response.warehouseId

    Given path '/api/v1/warehouses', warehouseId, 'inventory'
    And headers otherManagerHeaders
    When method get
    Then status 403
    And match response == apiErrorResponseContract
    And match response.error == 'FORBIDDEN'
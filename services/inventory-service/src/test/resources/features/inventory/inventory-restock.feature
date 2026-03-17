Feature: restock inventory

  Scenario: restock items in an existing warehouse
    * def setup = call read('classpath:features/inventory/_warehouse-setup.feature')
    * def warehouseId = setup.warehouseId

    Given url baseUrl
    And path '/api/v1/warehouses', warehouseId, 'inventory', 'restock'
    And request
    """
    {
      "items": [
        { "sku": "SKU-1", "quantity": 10 },
        { "sku": "SKU-2", "quantity": 5 }
      ]
    }
    """
    When method post
    Then status 200
    And match response == '#[]'
    And match response[0].warehouseId == warehouseId
    And match response[0].sku == 'SKU-1'
    And match response[0].quantity == 10
    And match response[0].reserved == 0
    And match response[0].available == 10

  Scenario: restock accumulates quantity on same sku
    * def setup = call read('classpath:features/inventory/_warehouse-setup.feature')
    * def warehouseId = setup.warehouseId

    Given url baseUrl
    And path '/api/v1/warehouses', warehouseId, 'inventory', 'restock'
    And request { "items": [{ "sku": "SKU-1", "quantity": 10 }] }
    When method post
    Then status 200

    Given url baseUrl
    And path '/api/v1/warehouses', warehouseId, 'inventory', 'restock'
    And request { "items": [{ "sku": "SKU-1", "quantity": 5 }] }
    When method post
    Then status 200
    And match response[0].quantity == 15
    And match response[0].available == 15

  Scenario: restock on non-existent warehouse returns 404
    Given url baseUrl
    And path '/api/v1/warehouses', 'non-existent-warehouse-00000', 'inventory', 'restock'
    And request { "items": [{ "sku": "SKU-1", "quantity": 10 }] }
    When method post
    Then status 404
    And match response.error == 'WAREHOUSE_NOT_FOUND'

Feature: get inventory by warehouse

  Background:
    * def setup = call read('classpath:features/inventory/_warehouse-setup.feature')
    * def warehouseId = setup.warehouseId

    Given url baseUrl
    And path '/api/v1/warehouses', warehouseId, 'inventory', 'restock'
    And request { "items": [{ "sku": "SKU-1", "quantity": 10 }] }
    When method post
    Then status 200

  Scenario: get inventory of a warehouse returns all its items
    Given url baseUrl
    And path '/api/v1/warehouses', warehouseId, 'inventory'
    When method get
    Then status 200
    And match response == '#[]'
    And match response[0].warehouseId == warehouseId
    And match response[0].sku == 'SKU-1'
    And match response[0].quantity == 10
    And match response[0].reserved == 0
    And match response[0].available == 10

  Scenario: get inventory of non-existent warehouse returns 404
    Given url baseUrl
    And path '/api/v1/warehouses', 'non-existent-warehouse-00000', 'inventory'
    When method get
    Then status 404
    And match response.error == 'WAREHOUSE_NOT_FOUND'

Feature: check inventory availability

  Background:
    * def setup = call read('classpath:features/inventory/_warehouse-setup.feature')
    * def warehouseId = setup.warehouseId

    Given url baseUrl
    And path '/api/v1/warehouses', warehouseId, 'inventory', 'restock'
    And request { "items": [{ "sku": "SKU-1", "quantity": 10 }] }
    When method post
    Then status 200

  Scenario: check availability with sufficient stock returns canFulfillAll true
    Given url baseUrl
    And path '/internal/v1/warehouses', warehouseId, 'inventory', 'availability'
    And request { "items": [{ "sku": "SKU-1", "quantity": 5 }] }
    When method post
    Then status 200
    And match response.canFulfillAll == true
    And match response.items[0].sku == 'SKU-1'
    And match response.items[0].required == 5
    And match response.items[0].canFulfill == true

  Scenario: check availability with insufficient stock returns canFulfillAll false
    Given url baseUrl
    And path '/internal/v1/warehouses', warehouseId, 'inventory', 'availability'
    And request { "items": [{ "sku": "SKU-1", "quantity": 100 }] }
    When method post
    Then status 200
    And match response.canFulfillAll == false
    And match response.items[0].sku == 'SKU-1'
    And match response.items[0].required == 100
    And match response.items[0].canFulfill == false

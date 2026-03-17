Feature: warehouse picking and packing flow

  Background:
    Given url baseUrl
    And path '/api/v1/warehouses'
    And request { "city": "Medellin", "lat": 6.2442, "lng": -75.5812 }
    When method post
    Then status 201
    * def warehouseId = response.warehouseId

  Scenario: complete picking returns 202
    * def orderId = 'order-' + java.util.UUID.randomUUID()
    Given url baseUrl
    And path '/api/v1/warehouses', warehouseId, 'orders', orderId, 'picking', 'complete'
    When method post
    Then status 202

  Scenario: complete packing returns 202
    * def orderId = 'order-' + java.util.UUID.randomUUID()
    Given url baseUrl
    And path '/api/v1/warehouses', warehouseId, 'orders', orderId, 'packing', 'complete'
    When method post
    Then status 202

  Scenario: picking on non-existent warehouse returns 404
    * def orderId = 'order-' + java.util.UUID.randomUUID()
    Given url baseUrl
    And path '/api/v1/warehouses', 'non-existent-wh-00000', 'orders', orderId, 'picking', 'complete'
    When method post
    Then status 404
    And match response.error == 'WAREHOUSE_NOT_FOUND'

  Scenario: packing on non-existent warehouse returns 404
    * def orderId = 'order-' + java.util.UUID.randomUUID()
    Given url baseUrl
    And path '/api/v1/warehouses', 'non-existent-wh-00000', 'orders', orderId, 'packing', 'complete'
    When method post
    Then status 404
    And match response.error == 'WAREHOUSE_NOT_FOUND'

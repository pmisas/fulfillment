@integration
Feature: warehouse operations integration

  Background:
    * url baseUrl
    * def admin = adminHeaders
    * def manager = managerHeaders
    * def operator = operatorHeaders
    * def orderId = 'order-it-001'

    Given path '/api/v1/warehouses'
    And headers admin
    And request { city: 'Cali Sur', lat: 3.4516, lng: -76.5320 }
    When method post
    Then status 201
    * def warehouseId = response.warehouseId

    Given path '/api/v1/warehouses', warehouseId, 'managers'
    And headers admin
    And request { userId: 'manager-1' }
    When method post
    Then status 201

  Scenario: assigned manager can complete picking
    Given path '/api/v1/warehouses', warehouseId, 'orders', orderId, 'picking', 'complete'
    And headers manager
    When method post
    Then status 202

  Scenario: assigned manager can complete packing
    Given path '/api/v1/warehouses', warehouseId, 'orders', orderId, 'packing', 'complete'
    And headers manager
    When method post
    Then status 202

  Scenario: user without access gets permission denied
    Given path '/api/v1/warehouses', warehouseId, 'orders', orderId, 'picking', 'complete'
    And headers operator
    When method post
    Then status 403
    And match response.error == 'FORBIDDEN'

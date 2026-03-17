Feature: get warehouse

  Scenario: get warehouse by id returns 200 with correct data
    Given url baseUrl
    And path '/api/v1/warehouses'
    And request { "city": "Medellin", "lat": 6.2442, "lng": -75.5812 }
    When method post
    Then status 201
    * def warehouseId = response.warehouseId

    Given url baseUrl
    And path '/api/v1/warehouses', warehouseId
    When method get
    Then status 200
    And match response.warehouseId == warehouseId
    And match response.city == 'medellin'
    And match response.lat == 6.2442
    And match response.lng == -75.5812

  Scenario: get non-existent warehouse returns 404
    Given url baseUrl
    And path '/api/v1/warehouses', 'non-existent-warehouse-00000'
    When method get
    Then status 404
    And match response.error == 'WAREHOUSE_NOT_FOUND'

  Scenario: get all warehouses returns array containing created warehouse
    Given url baseUrl
    And path '/api/v1/warehouses'
    And request { "city": "Barranquilla", "lat": 10.9685, "lng": -74.7813 }
    When method post
    Then status 201
    * def warehouseId = response.warehouseId

    Given url baseUrl
    And path '/api/v1/warehouses'
    When method get
    Then status 200
    And match response == '#[]'
    And match response[*].warehouseId contains warehouseId

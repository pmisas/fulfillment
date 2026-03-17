Feature: create warehouse

  Scenario: create a warehouse returns 201 with all fields
    Given url baseUrl
    And path '/api/v1/warehouses'
    And request { "city": "Medellin", "lat": 6.2442, "lng": -75.5812 }
    When method post
    Then status 201
    And match response.warehouseId == '#string'
    And match response.city == 'medellin'
    And match response.lat == 6.2442
    And match response.lng == -75.5812

  Scenario: each created warehouse gets a unique id
    Given url baseUrl
    And path '/api/v1/warehouses'
    And request { "city": "Bogota", "lat": 4.7110, "lng": -74.0721 }
    When method post
    Then status 201
    * def firstId = response.warehouseId

    Given url baseUrl
    And path '/api/v1/warehouses'
    And request { "city": "Cali", "lat": 3.4516, "lng": -76.5319 }
    When method post
    Then status 201
    And match response.warehouseId != firstId

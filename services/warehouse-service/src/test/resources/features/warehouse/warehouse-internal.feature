Feature: internal warehouse endpoints

  Scenario: HEAD existing warehouse returns 200
    Given url baseUrl
    And path '/api/v1/warehouses'
    And request { "city": "Medellin", "lat": 6.2442, "lng": -75.5812 }
    When method post
    Then status 201
    * def warehouseId = response.warehouseId

    Given url baseUrl
    And path '/internal/v1/warehouses', warehouseId
    When method head
    Then status 200

  Scenario: HEAD non-existent warehouse returns 404
    Given url baseUrl
    And path '/internal/v1/warehouses', 'non-existent-warehouse-00000'
    When method head
    Then status 404

  Scenario: GET internal warehouses returns array containing created warehouse
    Given url baseUrl
    And path '/api/v1/warehouses'
    And request { "city": "Cartagena", "lat": 10.3912, "lng": -75.4793 }
    When method post
    Then status 201
    * def warehouseId = response.warehouseId

    Given url baseUrl
    And path '/internal/v1/warehouses'
    When method get
    Then status 200
    And match response == '#[]'
    And match response[*].warehouseId contains warehouseId

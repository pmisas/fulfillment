Feature: warehouse creation validation

  Scenario: blank city returns 400 validation error
    Given url baseUrl
    And path '/api/v1/warehouses'
    And request { "city": "", "lat": 6.2442, "lng": -75.5812 }
    When method post
    Then status 400
    And match response.error == 'VALIDATION_ERROR'
    And match response.fields[*].field contains 'city'

  Scenario: null lat returns 400 validation error
    Given url baseUrl
    And path '/api/v1/warehouses'
    And request { "city": "Medellin", "lng": -75.5812 }
    When method post
    Then status 400
    And match response.error == 'VALIDATION_ERROR'
    And match response.fields[*].field contains 'lat'

  Scenario: null lng returns 400 validation error
    Given url baseUrl
    And path '/api/v1/warehouses'
    And request { "city": "Medellin", "lat": 6.2442 }
    When method post
    Then status 400
    And match response.error == 'VALIDATION_ERROR'
    And match response.fields[*].field contains 'lng'

  Scenario: empty request body returns 400 validation error
    Given url baseUrl
    And path '/api/v1/warehouses'
    And request {}
    When method post
    Then status 400
    And match response.error == 'VALIDATION_ERROR'

@contract
Feature: create warehouse contract

  Background:
    * url baseUrl

    * def createWarehouseRequestContract =
    """
    {
      city: '#string',
      lat: '#number',
      lng: '#number'
    }
    """

    * def warehouseResponseContract =
    """
    {
      warehouseId: '#string',
      city: '#string',
      lat: '#number',
      lng: '#number'
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

  Scenario: returns 201 when warehouse is created successfully
    * def payload =
    """
    {
      "city": "Bogota",
      "lat": 4.7110,
      "lng": -74.0721
    }
    """
    * match payload == createWarehouseRequestContract

    Given path '/api/v1/warehouses'
    And header Authorization = 'Bearer ' + adminToken
    And request payload
    When method post
    Then status 201
    And match response == warehouseResponseContract
    And match response.city == payload.city
    And match response.lat == payload.lat
    And match response.lng == payload.lng

  Scenario: returns 400 when request payload is invalid
    * def payload =
    """
    {
      "city": "",
      "lat": 4.7110,
      "lng": -74.0721
    }
    """

    Given path '/api/v1/warehouses'
    And header Authorization = 'Bearer ' + adminToken
    And request payload
    When method post
    Then status 400
    And match response == apiErrorResponseContract
    And match response.error == 'VALIDATION_ERROR'
    And match response.message == 'El request tiene campos inválidos.'

  Scenario: returns 403 when user is not allowed to create warehouses
    * def payload =
    """
    {
      "city": "Bogota",
      "lat": 4.7110,
      "lng": -74.0721
    }
    """
    * match payload == createWarehouseRequestContract

    Given path '/api/v1/warehouses'
    And header Authorization = 'Bearer ' + operatorAToken
    And request payload
    When method post
    Then status 403
    And match response == apiErrorResponseContract
    And match response.error == 'FORBIDDEN'
    
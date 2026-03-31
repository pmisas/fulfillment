@contract
Feature: get warehouse by id contract

  Background:
    * url baseUrl

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
      error: '#string',
      message: '#string'
    }
    """

  Scenario: returns 200 when warehouse exists and user has access
    * def createPayload =
    """
    {
      "city": "Medellin",
      "lat": 6.2442,
      "lng": -75.5812
    }
    """

    Given path '/api/v1/warehouses'
    And header Authorization = 'Bearer ' + adminToken
    And request createPayload
    When method post
    Then status 201
    * def warehouseId = response.warehouseId

    Given path '/api/v1/warehouses', warehouseId
    And header Authorization = 'Bearer ' + adminToken
    When method get
    Then status 200
    And match response == warehouseResponseContract
    And match response.warehouseId == warehouseId

  Scenario: returns 403 when user has no access to the warehouse
    * def createPayload =
    """
    {
      "city": "Cali",
      "lat": 3.4516,
      "lng": -76.5320
    }
    """

    Given path '/api/v1/warehouses'
    And header Authorization = 'Bearer ' + adminToken
    And request createPayload
    When method post
    Then status 201
    * def warehouseId = response.warehouseId

    Given path '/api/v1/warehouses', warehouseId
    And header Authorization = 'Bearer ' + warehouseManager2Token
    When method get
    Then status 403
    And match response == apiErrorResponseContract

  Scenario: returns 404 when warehouse does not exist
    Given path '/api/v1/warehouses', 'warehouse-not-found'
    And header Authorization = 'Bearer ' + adminToken
    When method get
    Then status 404
    And match response == apiErrorResponseContract
    
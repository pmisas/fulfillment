@contract
Feature: get all warehouses contract

  Background:
    * url baseUrl

    * def warehouseListContract = '#[] ##object'

    * def apiErrorResponseContract =
    """
    {
      error: '#string',
      message: '#string'
    }
    """

  Scenario: returns 200 with a list of warehouses
    Given path '/api/v1/warehouses'
    And header Authorization = 'Bearer ' + adminToken
    When method get
    Then status 200
    And match response == warehouseListContract
    And match each response contains
    """
    {
      warehouseId: '#string',
      city: '#string',
      lat: '#number',
      lng: '#number'
    }
    """

  Scenario: returns 403 when user is not allowed to list warehouses
    Given path '/api/v1/warehouses'
    And header Authorization = 'Bearer ' + operatorAToken
    When method get
    Then status 403
    And match response == apiErrorResponseContract
    
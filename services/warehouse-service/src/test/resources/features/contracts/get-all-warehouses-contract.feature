@contract
Feature: get all warehouses contract

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
      status: '#number',
      error: '#string',
      message: '#string',
      fields: '##[] ##object'
    }
    """

  Scenario: returns 200 with a list of warehouses
    Given path '/api/v1/warehouses'
    And header Authorization = 'Bearer ' + adminToken
    When method get
    Then status 200
    And match response == '#[] ##object'
    And match each response == warehouseResponseContract

  Scenario: returns 403 when user is not allowed to list warehouses
    Given path '/api/v1/warehouses'
    And header Authorization = 'Bearer ' + operatorAToken
    When method get
    Then status 403
    And match response == apiErrorResponseContract
    And match response.status == 403
    And match response.error == 'FORBIDDEN'
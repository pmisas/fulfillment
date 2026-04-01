@contract
Feature: get managers by warehouse contract

  Background:
    * url baseUrl

    * def warehouseManagersResponseContract =
    """
    {
      warehouseId: '#string',
      managerUserIds: '#[] #string'
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

  Scenario: returns 200 with managers assigned to the warehouse
    * def createWarehousePayload =
    """
    {
      "city": "Villavicencio",
      "lat": 4.1420,
      "lng": -73.6266
    }
    """

    Given path '/api/v1/warehouses'
    And header Authorization = 'Bearer ' + adminToken
    And request createWarehousePayload
    When method post
    Then status 201
    And match response.warehouseId == '#string'
    * def warehouseId = response.warehouseId

    Given path '/api/v1/warehouses', warehouseId, 'managers'
    And header Authorization = 'Bearer ' + adminToken
    And request
    """
    {
      "userId": "warehouse-manager-1"
    }
    """
    When method post
    Then status 201

    Given path '/api/v1/warehouses', warehouseId, 'managers'
    And header Authorization = 'Bearer ' + adminToken
    When method get
    Then status 200
    And match response == warehouseManagersResponseContract
    And match response.warehouseId == warehouseId
    And match response.managerUserIds contains 'warehouse-manager-1'

  Scenario: returns 403 when user is not allowed to consult managers
    Given path '/api/v1/warehouses', 'warehouse-001', 'managers'
    And header Authorization = 'Bearer ' + operatorAToken
    When method get
    Then status 403
    And match response == apiErrorResponseContract
    And match response.status == 403
    And match response.error == 'FORBIDDEN'

  Scenario: returns 404 when warehouse does not exist
    Given path '/api/v1/warehouses', 'warehouse-not-found', 'managers'
    And header Authorization = 'Bearer ' + adminToken
    When method get
    Then status 404
    And match response == apiErrorResponseContract
    And match response.status == 404
    And match response.error == 'WAREHOUSE_NOT_FOUND'
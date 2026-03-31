@contract
Feature: remove manager contract

  Background:
    * url baseUrl

    * def userWarehouseAccessResponseContract =
    """
    {
      userId: '#string',
      warehouseId: '#string',
      active: '#boolean',
      assignedAt: '#string',
      assignedBy: '##string',
      updatedAt: '#string'
    }
    """

    * def apiErrorResponseContract =
    """
    {
      error: '#string',
      message: '#string'
    }
    """

  Scenario: returns 200 when manager is removed successfully
    * def createWarehousePayload =
    """
    {
      "city": "Tunja",
      "lat": 5.5353,
      "lng": -73.3678
    }
    """

    Given path '/api/v1/warehouses'
    And header Authorization = 'Bearer ' + adminToken
    And request createWarehousePayload
    When method post
    Then status 201
    * def warehouseId = response.warehouseId

    * def payload =
    """
    {
      "userId": "warehouse-manager-1"
    }
    """

    Given path '/api/v1/warehouses', warehouseId, 'managers'
    And header Authorization = 'Bearer ' + adminToken
    And request payload
    When method post
    Then status 201

    Given path '/api/v1/warehouses', warehouseId, 'managers', 'warehouse-manager-1'
    And header Authorization = 'Bearer ' + adminToken
    When method delete
    Then status 200
    And match response == userWarehouseAccessResponseContract
    And match response.userId == 'warehouse-manager-1'
    And match response.warehouseId == warehouseId
    And match response.active == false

  Scenario: returns 403 when user is not allowed to remove managers
    Given path '/api/v1/warehouses', 'warehouse-001', 'managers', 'warehouse-manager-1'
    And header Authorization = 'Bearer ' + operatorAToken
    When method delete
    Then status 403
    And match response == apiErrorResponseContract

  Scenario: returns 404 when assignment or warehouse does not exist
    Given path '/api/v1/warehouses', 'warehouse-not-found', 'managers', 'user-not-found'
    And header Authorization = 'Bearer ' + adminToken
    When method delete
    Then status 404
    And match response == apiErrorResponseContract
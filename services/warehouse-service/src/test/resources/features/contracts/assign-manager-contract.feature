@contract
Feature: assign manager contract

  Background:
    * url baseUrl

    * def assignWarehouseManagerRequestContract =
    """
    {
      userId: '#string'
    }
    """

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

  Scenario: returns 201 when manager is assigned successfully
    * def createWarehousePayload =
    """
    {
      "city": "Pereira",
      "lat": 4.8143,
      "lng": -75.6946
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
    * match payload == assignWarehouseManagerRequestContract

    Given path '/api/v1/warehouses', warehouseId, 'managers'
    And header Authorization = 'Bearer ' + adminToken
    And request payload
    When method post
    Then status 201
    And match response == userWarehouseAccessResponseContract
    And match response.userId == payload.userId
    And match response.warehouseId == warehouseId
    And match response.active == true

  Scenario: returns 400 when request payload is invalid
    * def createWarehousePayload =
    """
    {
      "city": "Pasto",
      "lat": 1.2136,
      "lng": -77.2811
    }
    """

    Given path '/api/v1/warehouses'
    And header Authorization = 'Bearer ' + adminToken
    And request createWarehousePayload
    When method post
    Then status 201
    * def warehouseId = response.warehouseId

    Given path '/api/v1/warehouses', warehouseId, 'managers'
    And header Authorization = 'Bearer ' + adminToken
    And request { }
    When method post
    Then status 400
    And match response == apiErrorResponseContract

  Scenario: returns 403 when user is not allowed to assign managers
    * def createWarehousePayload =
    """
    {
      "city": "Manizales",
      "lat": 5.0703,
      "lng": -75.5138
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
      "userId": "warehouse-manager-2"
    }
    """
    * match payload == assignWarehouseManagerRequestContract

    Given path '/api/v1/warehouses', warehouseId, 'managers'
    And header Authorization = 'Bearer ' + operatorAToken
    And request payload
    When method post
    Then status 403
    And match response == apiErrorResponseContract

  Scenario: returns 404 when warehouse or user does not exist
    * def payload =
    """
    {
      "userId": "user-not-found"
    }
    """
    * match payload == assignWarehouseManagerRequestContract

    Given path '/api/v1/warehouses', 'warehouse-not-found', 'managers'
    And header Authorization = 'Bearer ' + adminToken
    And request payload
    When method post
    Then status 404
    And match response == apiErrorResponseContract
      Scenario: returns 409 when active manager assignment already exists
    * def createWarehousePayload =
    """
    {
      "city": "Neiva",
      "lat": 2.9273,
      "lng": -75.2819
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

    Given path '/api/v1/warehouses', warehouseId, 'managers'
    And header Authorization = 'Bearer ' + adminToken
    And request payload
    When method post
    Then status 409
    And match response == apiErrorResponseContract
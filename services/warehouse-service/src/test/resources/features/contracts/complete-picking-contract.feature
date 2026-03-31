@contract
Feature: complete picking contract

  Background:
    * url baseUrl

    * def apiErrorResponseContract =
    """
    {
      error: '#string',
      message: '#string'
    }
    """

  Scenario: returns 202 when picking completion is accepted
    * def createWarehousePayload =
    """
    {
      "city": "Cartagena",
      "lat": 10.3910,
      "lng": -75.4794
    }
    """

    Given path '/api/v1/warehouses'
    And header Authorization = 'Bearer ' + adminToken
    And request createWarehousePayload
    When method post
    Then status 201
    * def warehouseId = response.warehouseId

    * def orderId = 'order-test-001'

    Given path '/api/v1/warehouses', warehouseId, 'orders', orderId, 'picking', 'complete'
    And header Authorization = 'Bearer ' + adminToken
    When method post
    Then status 202

  Scenario: returns 403 when user has no access to warehouse for picking completion
    * def warehouseId = 'warehouse-001'
    * def orderId = 'order-test-001'

    Given path '/api/v1/warehouses', warehouseId, 'orders', orderId, 'picking', 'complete'
    And header Authorization = 'Bearer ' + warehouseManager2Token
    When method post
    Then status 403
    And match response == apiErrorResponseContract

  Scenario: returns 404 when warehouse or order does not exist
    Given path '/api/v1/warehouses', 'warehouse-not-found', 'orders', 'order-not-found', 'picking', 'complete'
    And header Authorization = 'Bearer ' + adminToken
    When method post
    Then status 404
    And match response == apiErrorResponseContract
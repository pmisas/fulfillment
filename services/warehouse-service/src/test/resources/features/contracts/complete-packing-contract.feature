@contract
Feature: complete packing contract

  Background:
    * url baseUrl

    * def apiErrorResponseContract =
    """
    {
      status: '#number',
      error: '#string',
      message: '#string',
      fields: '##[] ##object'
    }
    """

  Scenario: returns 202 when packing completion is accepted
    * def createWarehousePayload =
    """
    {
      "city": "Bucaramanga",
      "lat": 7.1193,
      "lng": -73.1227
    }
    """

    Given path '/api/v1/warehouses'
    And header Authorization = 'Bearer ' + adminToken
    And request createWarehousePayload
    When method post
    Then status 201
    * def warehouseId = response.warehouseId

    * def orderId = 'order-test-001'

    Given path '/api/v1/warehouses', warehouseId, 'orders', orderId, 'packing', 'complete'
    And header Authorization = 'Bearer ' + adminToken
    When method post
    Then status 202

  Scenario: returns 403 when user has no access to warehouse for packing completion
    * def warehouseId = 'warehouse-001'
    * def orderId = 'order-test-001'

    Given path '/api/v1/warehouses', warehouseId, 'orders', orderId, 'packing', 'complete'
    And header Authorization = 'Bearer ' + warehouseManager2Token
    When method post
    Then status 403
    And match response == apiErrorResponseContract
    And match response.error == 'FORBIDDEN'

  Scenario: returns 404 when warehouse or order does not exist
    Given path '/api/v1/warehouses', 'warehouse-not-found', 'orders', 'order-not-found', 'packing', 'complete'
    And header Authorization = 'Bearer ' + adminToken
    When method post
    Then status 404
    And match response == apiErrorResponseContract
    And match response.error == 'WAREHOUSE_NOT_FOUND'
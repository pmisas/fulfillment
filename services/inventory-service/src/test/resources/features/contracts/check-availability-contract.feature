@contract
Feature: check inventory availability contract

  Background:
    * url baseUrl

    * def batchRequestContract =
    """
    {
      items: '#[] ##object'
    }
    """

    * def batchItemContract =
    """
    {
      sku: '#string',
      quantity: '#number'
    }
    """

    * def checkAvailabilityResponseContract =
    """
    {
      canFulfillAll: '#boolean',
      items: '#[] ##object'
    }
    """

    * def itemAvailabilityContract =
    """
    {
      sku: '#string',
      required: '#number',
      available: '#number',
      canFulfill: '#boolean'
    }
    """

  Scenario: returns 200 with availability result for requested items
    * def createWarehousePayload =
    """
    {
      "city": "Bogota",
      "lat": 4.7110,
      "lng": -74.0721
    }
    """
    * def assignManagerPayload =
    """
    {
      "userId": "warehouse-manager-1"
    }
    """
    * def restockPayload =
    """
    {
      "items": [
        { "sku": "SKU-1", "quantity": 10 },
        { "sku": "SKU-2", "quantity": 5 }
      ]
    }
    """
    * def payload =
    """
    {
      "items": [
        { "sku": "SKU-1", "quantity": 4 },
        { "sku": "SKU-2", "quantity": 2 }
      ]
    }
    """

    * match payload == batchRequestContract
    * match each payload.items == batchItemContract

    Given path '/api/v1/warehouses'
    And header Authorization = 'Bearer ' + adminToken
    And request createWarehousePayload
    When method post
    Then status 201
    * def warehouseId = response.warehouseId

    Given path '/api/v1/warehouses', warehouseId, 'managers'
    And header Authorization = 'Bearer ' + adminToken
    And request assignManagerPayload
    When method post
    Then status 201

    Given path '/api/v1/warehouses', warehouseId, 'inventory', 'restock'
    And header Authorization = 'Bearer ' + warehouseManagerToken
    And request restockPayload
    When method post
    Then status 200

    Given path '/internal/v1/warehouses', warehouseId, 'inventory', 'availability'
    And request payload
    When method post
    Then status 200
    And match response == checkAvailabilityResponseContract
    And match each response.items == itemAvailabilityContract
    And match response.canFulfillAll == true
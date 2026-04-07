@integration
Feature: inventory availability and reservation integration

  Background:
    * url baseUrl

    * def createWarehousePayload =
    """
    {
      "city": "Cali",
      "lat": 3.4516,
      "lng": -76.5320
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

    * def availabilityPayload =
    """
    {
      "items": [
        { "sku": "SKU-1", "quantity": 4 },
        { "sku": "SKU-2", "quantity": 2 }
      ]
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

  Scenario: service checks availability and creates a reservation successfully
    Given path '/api/v1/warehouses'
    And headers adminHeaders
    And request createWarehousePayload
    When method post
    Then status 201
    And match response.warehouseId == '#string'
    * def warehouseId = response.warehouseId

    Given path '/api/v1/warehouses', warehouseId, 'managers'
    And headers adminHeaders
    And request assignManagerPayload
    When method post
    Then status 201

    Given path '/api/v1/warehouses', warehouseId, 'inventory', 'restock'
    And headers managerHeaders
    And request restockPayload
    When method post
    Then status 200

    Given path '/internal/v1/warehouses', warehouseId, 'inventory', 'availability'
    And request availabilityPayload
    When method post
    Then status 200
    And match response == checkAvailabilityResponseContract
    And match each response.items == itemAvailabilityContract
    And match response.canFulfillAll == true

    * def reservationId = 'res-' + java.util.UUID.randomUUID()
    * def orderId = 'order-' + java.util.UUID.randomUUID()

    Given path '/internal/v1/warehouses', warehouseId, 'reservations'
    And request
    """
    {
      "reservationId": "#(reservationId)",
      "orderId": "#(orderId)",
      "items": [
        { "sku": "SKU-1", "quantity": 4 },
        { "sku": "SKU-2", "quantity": 2 }
      ]
    }
    """
    When method post
    Then status 201

    Given path '/internal/v1/warehouses', warehouseId, 'reservations'
    And request
    """
    {
      "reservationId": "#(reservationId)",
      "orderId": "#(orderId)",
      "items": [
        { "sku": "SKU-1", "quantity": 4 },
        { "sku": "SKU-2", "quantity": 2 }
      ]
    }
    """
    When method post
    Then status 200

  Scenario: reservation returns 422 when stock is insufficient
    Given path '/api/v1/warehouses'
    And headers adminHeaders
    And request createWarehousePayload
    When method post
    Then status 201
    And match response.warehouseId == '#string'
    * def warehouseId = response.warehouseId

    Given path '/api/v1/warehouses', warehouseId, 'managers'
    And headers adminHeaders
    And request assignManagerPayload
    When method post
    Then status 201

    Given path '/api/v1/warehouses', warehouseId, 'inventory', 'restock'
    And headers managerHeaders
    And request
    """
    {
      "items": [
        { "sku": "SKU-1", "quantity": 3 }
      ]
    }
    """
    When method post
    Then status 200

    * def reservationId = 'res-' + java.util.UUID.randomUUID()
    * def orderId = 'order-' + java.util.UUID.randomUUID()

    Given path '/internal/v1/warehouses', warehouseId, 'reservations'
    And request
    """
    {
      "reservationId": "#(reservationId)",
      "orderId": "#(orderId)",
      "items": [
        { "sku": "SKU-1", "quantity": 5 }
      ]
    }
    """
    When method post
    Then status 422
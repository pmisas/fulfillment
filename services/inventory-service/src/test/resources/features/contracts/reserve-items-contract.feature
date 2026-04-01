@contract
Feature: reserve items contract

  Background:
    * url baseUrl

    * def reserveItemsRequestContract =
    """
    {
      reservationId: '#string',
      orderId: '#string',
      items: '#[] ##object'
    }
    """

    * def reserveItemContract =
    """
    {
      sku: '#string',
      quantity: '#number'
    }
    """

  Scenario: returns 201 when reservation is created successfully
    * def createWarehousePayload =
    """
    {
      "city": "Medellin",
      "lat": 6.2442,
      "lng": -75.5812
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
        { "sku": "SKU-1", "quantity": 10 }
      ]
    }
    """
    * def reservationId = 'res-' + java.util.UUID.randomUUID()
    * def orderId = 'order-' + java.util.UUID.randomUUID()
    * def payload =
    """
    {
      "reservationId": "#(reservationId)",
      "orderId": "#(orderId)",
      "items": [
        { "sku": "SKU-1", "quantity": 4 }
      ]
    }
    """

    * match payload == reserveItemsRequestContract
    * match each payload.items == reserveItemContract

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

    Given path '/internal/v1/warehouses', warehouseId, 'reservations'
    And request payload
    When method post
    Then status 201

  Scenario: returns 200 when reservation already exists
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
        { "sku": "SKU-1", "quantity": 10 }
      ]
    }
    """
    * def reservationId = 'res-' + java.util.UUID.randomUUID()
    * def orderId = 'order-' + java.util.UUID.randomUUID()
    * def payload =
    """
    {
      "reservationId": "#(reservationId)",
      "orderId": "#(orderId)",
      "items": [
        { "sku": "SKU-1", "quantity": 4 }
      ]
    }
    """

    * match payload == reserveItemsRequestContract
    * match each payload.items == reserveItemContract

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

    Given path '/internal/v1/warehouses', warehouseId, 'reservations'
    And request payload
    When method post
    Then status 201

    Given path '/internal/v1/warehouses', warehouseId, 'reservations'
    And request payload
    When method post
    Then status 200

  Scenario: returns 422 when stock is insufficient
    * def createWarehousePayload =
    """
    {
      "city": "Barranquilla",
      "lat": 10.9685,
      "lng": -74.7813
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
        { "sku": "SKU-1", "quantity": 3 }
      ]
    }
    """
    * def reservationId = 'res-' + java.util.UUID.randomUUID()
    * def orderId = 'order-' + java.util.UUID.randomUUID()
    * def payload =
    """
    {
      "reservationId": "#(reservationId)",
      "orderId": "#(orderId)",
      "items": [
        { "sku": "SKU-1", "quantity": 5 }
      ]
    }
    """

    * match payload == reserveItemsRequestContract
    * match each payload.items == reserveItemContract

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

    Given path '/internal/v1/warehouses', warehouseId, 'reservations'
    And request payload
    When method post
    Then status 422
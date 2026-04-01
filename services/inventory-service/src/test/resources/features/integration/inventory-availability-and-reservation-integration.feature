@integration
Feature: inventory availability and reservation integration

  Background:
    * url baseUrl
    * def admin = adminToken
    * def manager = warehouseManagerToken

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

    * def availabilityPayload =
    """
    {
      "items": [
        { "sku": "SKU-1", "quantity": 4 },
        { "sku": "SKU-2", "quantity": 2 }
      ]
    }
    """

    * def insufficientAvailabilityPayload =
    """
    {
      "items": [
        { "sku": "SKU-1", "quantity": 100 }
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

    * def reserveItemsPayloadTemplate =
    """
    {
      reservationId: '#(reservationId)',
      orderId: '#(orderId)',
      items: '#(items)'
    }
    """

  Scenario: check availability and reserve inventory successfully
    # create warehouse
    Given path '/api/v1/warehouses'
    And header Authorization = 'Bearer ' + admin
    And request createWarehousePayload
    When method post
    Then status 201
    * def warehouseId = response.warehouseId

    # assign manager
    Given path '/api/v1/warehouses', warehouseId, 'managers'
    And header Authorization = 'Bearer ' + admin
    And request assignManagerPayload
    When method post
    Then status 201

    # restock inventory
    Given path '/api/v1/warehouses', warehouseId, 'inventory', 'restock'
    And header Authorization = 'Bearer ' + manager
    And request restockPayload
    When method post
    Then status 200

    # check availability
    Given path '/internal/v1/warehouses', warehouseId, 'inventory', 'availability'
    And request availabilityPayload
    When method post
    Then status 200
    And match response == checkAvailabilityResponseContract
    And match each response.items == itemAvailabilityContract
    And match response.canFulfillAll == true
    * def availabilitySku1 = karate.filter(response.items, function(x){ return x.sku == 'SKU-1' })[0]
    * def availabilitySku2 = karate.filter(response.items, function(x){ return x.sku == 'SKU-2' })[0]
    And match availabilitySku1.required == 4
    And match availabilitySku1.available == 10
    And match availabilitySku1.canFulfill == true
    And match availabilitySku2.required == 2
    And match availabilitySku2.available == 5
    And match availabilitySku2.canFulfill == true

    # reserve inventory
    * def reservationId = 'res-' + java.util.UUID.randomUUID()
    * def orderId = 'order-' + java.util.UUID.randomUUID()
    * def items = availabilityPayload.items
    * def reservePayload = { reservationId: '#(reservationId)', orderId: '#(orderId)', items: '#(items)' }

    Given path '/internal/v1/warehouses', warehouseId, 'reservations'
    And request reservePayload
    When method post
    Then status 201

    # same reservation again should be idempotent
    Given path '/internal/v1/warehouses', warehouseId, 'reservations'
    And request reservePayload
    When method post
    Then status 200

  Scenario: reserve inventory returns 422 when stock is insufficient
    Given path '/api/v1/warehouses'
    And header Authorization = 'Bearer ' + admin
    And request createWarehousePayload
    When method post
    Then status 201
    * def warehouseId = response.warehouseId

    Given path '/api/v1/warehouses', warehouseId, 'managers'
    And header Authorization = 'Bearer ' + admin
    And request assignManagerPayload
    When method post
    Then status 201

    Given path '/api/v1/warehouses', warehouseId, 'inventory', 'restock'
    And header Authorization = 'Bearer ' + manager
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
    * def reservePayload =
    """
    {
      "reservationId": "#(reservationId)",
      "orderId": "#(orderId)",
      "items": [
        { "sku": "SKU-1", "quantity": 5 }
      ]
    }
    """

    Given path '/internal/v1/warehouses', warehouseId, 'reservations'
    And request reservePayload
    When method post
    Then status 422
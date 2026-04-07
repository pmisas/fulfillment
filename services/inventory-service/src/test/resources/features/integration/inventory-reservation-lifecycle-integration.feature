@integration
Feature: inventory reservation lifecycle integration

  Background:
    * url baseUrl

    * def createWarehousePayload =
    """
    {
      "city": "Pereira",
      "lat": 4.8143,
      "lng": -75.6946
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

  Scenario: reservation can be consumed successfully
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

    * def reservationId = 'res-' + java.util.UUID.randomUUID()
    * def orderId = 'order-' + java.util.UUID.randomUUID()

    Given path '/internal/v1/warehouses', warehouseId, 'reservations'
    And request
    """
    {
      "reservationId": "#(reservationId)",
      "orderId": "#(orderId)",
      "items": [
        { "sku": "SKU-1", "quantity": 4 }
      ]
    }
    """
    When method post
    Then status 201

    Given path '/internal/v1/reservations', reservationId, 'consume'
    When method post
    Then status 204

  Scenario: reservation can be released successfully
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

    * def reservationId = 'res-' + java.util.UUID.randomUUID()
    * def orderId = 'order-' + java.util.UUID.randomUUID()

    Given path '/internal/v1/warehouses', warehouseId, 'reservations'
    And request
    """
    {
      "reservationId": "#(reservationId)",
      "orderId": "#(orderId)",
      "items": [
        { "sku": "SKU-1", "quantity": 3 }
      ]
    }
    """
    When method post
    Then status 201

    Given path '/internal/v1/reservations', reservationId
    When method delete
    Then status 204

  Scenario: consuming a non-existent reservation returns 404
    Given path '/internal/v1/reservations', 'reservation-not-found', 'consume'
    When method post
    Then status 404
@integration
Feature: inventory reservation lifecycle integration

  Background:
    * url baseUrl
    * def admin = adminToken
    * def manager = warehouseManagerToken

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

  Scenario: reserve and consume inventory successfully
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

  Scenario: consume reservation returns 404 when reservation does not exist
    Given path '/internal/v1/reservations', 'reservation-not-found', 'consume'
    When method post
    Then status 404

  Scenario: reserve and release inventory successfully
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
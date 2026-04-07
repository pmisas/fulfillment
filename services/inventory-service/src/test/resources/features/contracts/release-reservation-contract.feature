@contract
Feature: release reservation contract

  Background:
    * url baseUrl

  Scenario: returns 204 when reservation is released successfully
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
    * def reservationId = 'res-' + java.util.UUID.randomUUID()
    * def orderId = 'order-' + java.util.UUID.randomUUID()

    Given path '/api/v1/warehouses'
    And header Authorization = 'Bearer ' + adminToken
    And request createWarehousePayload
    When method post
    Then status 201
    And match response.warehouseId == '#string'
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

    Given path '/internal/v1/reservations', reservationId
    When method delete
    Then status 204
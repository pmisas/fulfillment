Feature: reserve inventory items

  Background:
    * def setup = call read('classpath:features/inventory/_warehouse-setup.feature')
    * def warehouseId = setup.warehouseId

    Given url baseUrl
    And path '/api/v1/warehouses', warehouseId, 'inventory', 'restock'
    And request { "items": [{ "sku": "SKU-1", "quantity": 10 }] }
    When method post
    Then status 200

  Scenario: reserve items with sufficient stock returns 201
    Given url baseUrl
    And path '/internal/v1/warehouses', warehouseId, 'reservations'
    And request
    """
    {
      "reservationId": "#('res-' + java.util.UUID.randomUUID())",
      "orderId": "#('order-' + java.util.UUID.randomUUID())",
      "items": [{ "sku": "SKU-1", "quantity": 3 }]
    }
    """
    When method post
    Then status 201

  Scenario: reserve with same reservationId returns 200
    * def reservationId = 'res-' + java.util.UUID.randomUUID()
    * def orderId = 'order-' + java.util.UUID.randomUUID()

    Given url baseUrl
    And path '/internal/v1/warehouses', warehouseId, 'reservations'
    And request { "reservationId": "#(reservationId)", "orderId": "#(orderId)", "items": [{ "sku": "SKU-1", "quantity": 3 }] }
    When method post
    Then status 201

    Given url baseUrl
    And path '/internal/v1/warehouses', warehouseId, 'reservations'
    And request { "reservationId": "#(reservationId)", "orderId": "#(orderId)", "items": [{ "sku": "SKU-1", "quantity": 3 }] }
    When method post
    Then status 200

  Scenario: reserve more than available stock returns 422
    Given url baseUrl
    And path '/internal/v1/warehouses', warehouseId, 'reservations'
    And request
    """
    {
      "reservationId": "#('res-' + java.util.UUID.randomUUID())",
      "orderId": "#('order-' + java.util.UUID.randomUUID())",
      "items": [{ "sku": "SKU-1", "quantity": 999 }]
    }
    """
    When method post
    Then status 422

Feature: reservation lifecycle - release and consume

  Background:
    * def setup = call read('classpath:features/inventory/_warehouse-setup.feature')
    * def warehouseId = setup.warehouseId

    Given url baseUrl
    And path '/api/v1/warehouses', warehouseId, 'inventory', 'restock'
    And request { "items": [{ "sku": "SKU-1", "quantity": 10 }] }
    When method post
    Then status 200

  Scenario: release a reservation returns 204
    * def reservationId = 'res-' + java.util.UUID.randomUUID()
    * def orderId = 'order-' + java.util.UUID.randomUUID()

    Given url baseUrl
    And path '/internal/v1/warehouses', warehouseId, 'reservations'
    And request { "reservationId": "#(reservationId)", "orderId": "#(orderId)", "items": [{ "sku": "SKU-1", "quantity": 3 }] }
    When method post
    Then status 201

    Given url baseUrl
    And path '/internal/v1/reservations', reservationId
    When method delete
    Then status 204

  Scenario: consume a reservation returns 204
    * def reservationId = 'res-' + java.util.UUID.randomUUID()
    * def orderId = 'order-' + java.util.UUID.randomUUID()

    Given url baseUrl
    And path '/internal/v1/warehouses', warehouseId, 'reservations'
    And request { "reservationId": "#(reservationId)", "orderId": "#(orderId)", "items": [{ "sku": "SKU-1", "quantity": 3 }] }
    When method post
    Then status 201

    Given url baseUrl
    And path '/internal/v1/reservations', reservationId, 'consume'
    When method post
    Then status 204

  Scenario: consume non-existent reservation returns 404
    Given url baseUrl
    And path '/internal/v1/reservations', 'non-existent-res-00000', 'consume'
    When method post
    Then status 404

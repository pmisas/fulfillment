Feature: create shipment idempotency

  Scenario: creating shipment with same orderId returns same shipmentId
    * def orderId = 'order-' + java.util.UUID.randomUUID()
    * def warehouseId = 'warehouse-' + java.util.UUID.randomUUID()

    Given url baseUrl
    And path '/internal/v1/shipments'
    And request { "orderId": "#(orderId)", "warehouseId": "#(warehouseId)", "items": [{ "sku": "SKU-1", "quantity": 2 }] }
    When method post
    Then status 201
    * def firstShipmentId = response.shipmentId

    Given url baseUrl
    And path '/internal/v1/shipments'
    And request { "orderId": "#(orderId)", "warehouseId": "#(warehouseId)", "items": [{ "sku": "SKU-1", "quantity": 2 }] }
    When method post
    Then status 201
    And match response.shipmentId == firstShipmentId

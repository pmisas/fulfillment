Feature: create shipment

  Scenario: create a shipment returns 201 with correct fields
    * def orderId = 'order-' + java.util.UUID.randomUUID()
    * def warehouseId = 'warehouse-' + java.util.UUID.randomUUID()
    Given url baseUrl
    And path '/internal/v1/shipments'
    And request
    """
    {
      "orderId": "#(orderId)",
      "warehouseId": "#(warehouseId)",
      "items": [
        { "sku": "SKU-1", "quantity": 2 },
        { "sku": "SKU-2", "quantity": 5 }
      ]
    }
    """
    When method post
    Then status 201
    And match response.shipmentId == '#string'
    And match response.orderId == orderId
    And match response.warehouseId == warehouseId
    And match response.carrier == 'INTERNAL_CARRIER'
    And match response.status == 'PENDING'
    And match response.trackingId == '#null'
    And match response.items == '#[2]'
    And match response.createdAt == '#string'
    And match response.estimatedDeliveryAt == '#string'
    And match response.shippingGuideS3Key == '#string'

  Scenario: creating shipment generates shipping guide immediately
    * def orderId = 'order-' + java.util.UUID.randomUUID()
    * def warehouseId = 'warehouse-' + java.util.UUID.randomUUID()
    Given url baseUrl
    And path '/internal/v1/shipments'
    And request { "orderId": "#(orderId)", "warehouseId": "#(warehouseId)", "items": [{ "sku": "SKU-1", "quantity": 1 }] }
    When method post
    Then status 201
    And match response.shippingGuideS3Key == '#notnull'

  Scenario: each created shipment gets a unique id
    * def orderId1 = 'order-' + java.util.UUID.randomUUID()
    * def orderId2 = 'order-' + java.util.UUID.randomUUID()
    * def warehouseId = 'warehouse-' + java.util.UUID.randomUUID()

    Given url baseUrl
    And path '/internal/v1/shipments'
    And request { "orderId": "#(orderId1)", "warehouseId": "#(warehouseId)", "items": [{ "sku": "SKU-1", "quantity": 1 }] }
    When method post
    Then status 201
    * def firstId = response.shipmentId

    Given url baseUrl
    And path '/internal/v1/shipments'
    And request { "orderId": "#(orderId2)", "warehouseId": "#(warehouseId)", "items": [{ "sku": "SKU-1", "quantity": 1 }] }
    When method post
    Then status 201
    And match response.shipmentId != firstId

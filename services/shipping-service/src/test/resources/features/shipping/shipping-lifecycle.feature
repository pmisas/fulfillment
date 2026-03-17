Feature: shipment lifecycle - ship and deliver

  Scenario: mark shipment as shipped returns 200 with SHIPPED status
    * def orderId = 'order-' + java.util.UUID.randomUUID()
    * def warehouseId = 'warehouse-' + java.util.UUID.randomUUID()
    * def created = call read('classpath:features/shipping/_create-shipment.feature') { orderId: '#(orderId)', warehouseId: '#(warehouseId)' }
    * def shipmentId = created.shipmentId

    Given url baseUrl
    And path '/api/v1/shipments', shipmentId, 'ship'
    When method post
    Then status 200
    And match response.status == 'SHIPPED'
    And match response.shipmentId == shipmentId
    And match response.shippedAt == '#string'

  Scenario: full flow PENDING to SHIPPED to DELIVERED
    * def orderId = 'order-' + java.util.UUID.randomUUID()
    * def warehouseId = 'warehouse-' + java.util.UUID.randomUUID()
    * def created = call read('classpath:features/shipping/_create-shipment.feature') { orderId: '#(orderId)', warehouseId: '#(warehouseId)' }
    * def shipmentId = created.shipmentId

    Given url baseUrl
    And path '/api/v1/shipments', shipmentId, 'ship'
    When method post
    Then status 200
    And match response.status == 'SHIPPED'

    Given url baseUrl
    And path '/api/v1/shipments', shipmentId, 'deliver'
    When method post
    Then status 200
    And match response.status == 'DELIVERED'

  Scenario: mark as shipped is idempotent when already SHIPPED
    * def orderId = 'order-' + java.util.UUID.randomUUID()
    * def warehouseId = 'warehouse-' + java.util.UUID.randomUUID()
    * def created = call read('classpath:features/shipping/_create-shipment.feature') { orderId: '#(orderId)', warehouseId: '#(warehouseId)' }
    * def shipmentId = created.shipmentId

    Given url baseUrl
    And path '/api/v1/shipments', shipmentId, 'ship'
    When method post
    Then status 200

    Given url baseUrl
    And path '/api/v1/shipments', shipmentId, 'ship'
    When method post
    Then status 200
    And match response.status == 'SHIPPED'

  Scenario: invalid transition DELIVERED to SHIPPED returns 400
    * def orderId = 'order-' + java.util.UUID.randomUUID()
    * def warehouseId = 'warehouse-' + java.util.UUID.randomUUID()
    * def created = call read('classpath:features/shipping/_create-shipment.feature') { orderId: '#(orderId)', warehouseId: '#(warehouseId)' }
    * def shipmentId = created.shipmentId

    Given url baseUrl
    And path '/api/v1/shipments', shipmentId, 'ship'
    When method post
    Then status 200

    Given url baseUrl
    And path '/api/v1/shipments', shipmentId, 'deliver'
    When method post
    Then status 200

    Given url baseUrl
    And path '/api/v1/shipments', shipmentId, 'ship'
    When method post
    Then status 400
    And match response.error == 'INVALID_STATUS_TRANSITION'

  Scenario: ship non-existent shipment returns 404
    Given url baseUrl
    And path '/api/v1/shipments', 'non-existent-shipment-00000', 'ship'
    When method post
    Then status 404
    And match response.error == 'SHIPMENT_NOT_FOUND'

  Scenario: deliver non-existent shipment returns 404
    Given url baseUrl
    And path '/api/v1/shipments', 'non-existent-shipment-00000', 'deliver'
    When method post
    Then status 404
    And match response.error == 'SHIPMENT_NOT_FOUND'

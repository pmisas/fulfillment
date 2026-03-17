Feature: get shipping guide URL

  Scenario: get shipping guide of created shipment returns url and expiresAt
    * def orderId = 'order-' + java.util.UUID.randomUUID()
    * def warehouseId = 'warehouse-' + java.util.UUID.randomUUID()
    * def created = call read('classpath:features/shipping/_create-shipment.feature') { orderId: '#(orderId)', warehouseId: '#(warehouseId)' }
    * def shipmentId = created.shipmentId

    Given url baseUrl
    And path '/api/v1/shipments', shipmentId, 'guide'
    When method get
    Then status 200
    And match response.url == '#string'
    And match response.expiresAt == '#string'

  Scenario: get guide of non-existent shipment returns 404
    Given url baseUrl
    And path '/api/v1/shipments', 'non-existent-shipment-00000', 'guide'
    When method get
    Then status 404
    And match response.error == 'SHIPMENT_NOT_FOUND'

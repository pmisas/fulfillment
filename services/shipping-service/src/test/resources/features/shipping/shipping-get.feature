Feature: get shipment

  Scenario: get shipment by id returns 200 with correct data
    * def orderId = 'order-' + java.util.UUID.randomUUID()
    * def warehouseId = 'warehouse-' + java.util.UUID.randomUUID()
    * def created = call read('classpath:features/shipping/_create-shipment.feature') { orderId: '#(orderId)', warehouseId: '#(warehouseId)' }
    * def shipmentId = created.shipmentId

    Given url baseUrl
    And path '/api/v1/shipments', shipmentId
    When method get
    Then status 200
    And match response.shipmentId == shipmentId
    And match response.orderId == orderId
    And match response.warehouseId == warehouseId
    And match response.status == 'PENDING'
    And match response.carrier == 'INTERNAL_CARRIER'

  Scenario: get non-existent shipment returns 404
    Given url baseUrl
    And path '/api/v1/shipments', 'non-existent-shipment-00000'
    When method get
    Then status 404
    And match response.error == 'SHIPMENT_NOT_FOUND'

  Scenario: get all shipments returns array containing created shipment
    * def orderId = 'order-' + java.util.UUID.randomUUID()
    * def warehouseId = 'warehouse-' + java.util.UUID.randomUUID()
    * def created = call read('classpath:features/shipping/_create-shipment.feature') { orderId: '#(orderId)', warehouseId: '#(warehouseId)' }
    * def shipmentId = created.shipmentId

    Given url baseUrl
    And path '/api/v1/shipments'
    When method get
    Then status 200
    And match response == '#[]'
    And match response[*].shipmentId contains shipmentId

  Scenario: filter shipments by orderId returns only that shipment
    * def orderId = 'order-' + java.util.UUID.randomUUID()
    * def warehouseId = 'warehouse-' + java.util.UUID.randomUUID()
    * def created = call read('classpath:features/shipping/_create-shipment.feature') { orderId: '#(orderId)', warehouseId: '#(warehouseId)' }

    Given url baseUrl
    And path '/api/v1/shipments'
    And param orderId = orderId
    When method get
    Then status 200
    And match response == '#[1]'
    And match response[0].orderId == orderId

Feature: create shipment validation

  Scenario: blank orderId returns 400 validation error
    Given url baseUrl
    And path '/internal/v1/shipments'
    And request { "orderId": "", "warehouseId": "warehouse-001", "items": [{ "sku": "SKU-1", "quantity": 1 }] }
    When method post
    Then status 400
    And match response.error == 'VALIDATION_ERROR'
    And match response.fields[*].field contains 'orderId'

  Scenario: blank warehouseId returns 400 validation error
    Given url baseUrl
    And path '/internal/v1/shipments'
    And request { "orderId": "order-001", "warehouseId": "", "items": [{ "sku": "SKU-1", "quantity": 1 }] }
    When method post
    Then status 400
    And match response.error == 'VALIDATION_ERROR'
    And match response.fields[*].field contains 'warehouseId'

  Scenario: empty items list returns 400 validation error
    Given url baseUrl
    And path '/internal/v1/shipments'
    And request { "orderId": "order-001", "warehouseId": "warehouse-001", "items": [] }
    When method post
    Then status 400
    And match response.error == 'VALIDATION_ERROR'
    And match response.fields[*].field contains 'items'

  Scenario: item with blank sku returns 400 validation error
    Given url baseUrl
    And path '/internal/v1/shipments'
    And request { "orderId": "order-001", "warehouseId": "warehouse-001", "items": [{ "sku": "", "quantity": 1 }] }
    When method post
    Then status 400
    And match response.error == 'VALIDATION_ERROR'

  Scenario: item with zero quantity returns 400 validation error
    Given url baseUrl
    And path '/internal/v1/shipments'
    And request { "orderId": "order-001", "warehouseId": "warehouse-001", "items": [{ "sku": "SKU-1", "quantity": 0 }] }
    When method post
    Then status 400
    And match response.error == 'VALIDATION_ERROR'

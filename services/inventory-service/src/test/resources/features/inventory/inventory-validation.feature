Feature: inventory validation errors

  # La validacion del @RequestBody ocurre antes de la logica del servicio,
  # por eso estos tests no necesitan una bodega real.

  Scenario: restock with empty items list returns 400
    Given url baseUrl
    And path '/api/v1/warehouses', 'any-warehouse', 'inventory', 'restock'
    And request { "items": [] }
    When method post
    Then status 400
    And match response.error == 'VALIDATION_ERROR'

  Scenario: restock with blank sku returns 400
    Given url baseUrl
    And path '/api/v1/warehouses', 'any-warehouse', 'inventory', 'restock'
    And request { "items": [{ "sku": "", "quantity": 5 }] }
    When method post
    Then status 400
    And match response.error == 'VALIDATION_ERROR'
    And match response.fields == '#[]'

  Scenario: restock with quantity zero returns 400
    Given url baseUrl
    And path '/api/v1/warehouses', 'any-warehouse', 'inventory', 'restock'
    And request { "items": [{ "sku": "SKU-1", "quantity": 0 }] }
    When method post
    Then status 400
    And match response.error == 'VALIDATION_ERROR'

  Scenario: reserve with missing reservationId returns 400
    Given url baseUrl
    And path '/internal/v1/warehouses', 'any-warehouse', 'reservations'
    And request { "orderId": "order-001", "items": [{ "sku": "SKU-1", "quantity": 1 }] }
    When method post
    Then status 400
    And match response.error == 'VALIDATION_ERROR'

  Scenario: reserve with empty items list returns 400
    Given url baseUrl
    And path '/internal/v1/warehouses', 'any-warehouse', 'reservations'
    And request { "reservationId": "res-001", "orderId": "order-001", "items": [] }
    When method post
    Then status 400
    And match response.error == 'VALIDATION_ERROR'

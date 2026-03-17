@ignore
Feature: helper - create a shipment

  Scenario: create shipment
    Given url baseUrl
    And path '/internal/v1/shipments'
    And request
    """
    {
      "orderId": "#(orderId)",
      "warehouseId": "#(warehouseId)",
      "items": [{ "sku": "SKU-1", "quantity": 2 }]
    }
    """
    When method post
    Then status 201
    * def shipmentId = response.shipmentId

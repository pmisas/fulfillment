@integration
Feature: inventory restock and query integration

  Background:
    * url baseUrl

    * def createWarehousePayload =
    """
    {
      "city": "Bogota",
      "lat": 4.7110,
      "lng": -74.0721
    }
    """

    * def assignManagerPayload =
    """
    {
      "userId": "warehouse-manager-1"
    }
    """

    * def restockPayload =
    """
    {
      "items": [
        { "sku": "SKU-1", "quantity": 10 },
        { "sku": "SKU-2", "quantity": 5 }
      ]
    }
    """

    * def inventoryItemContract =
    """
    {
      warehouseId: '#string',
      sku: '#string',
      quantity: '#number',
      reserved: '#number',
      available: '#number',
      updateAt: '#string'
    }
    """

  Scenario: manager restocks inventory and can query the updated inventory
    Given path '/api/v1/warehouses'
    And headers adminHeaders
    And request createWarehousePayload
    When method post
    Then status 201
    And match response.warehouseId == '#string'
    * def warehouseId = response.warehouseId

    Given path '/api/v1/warehouses', warehouseId, 'managers'
    And headers adminHeaders
    And request assignManagerPayload
    When method post
    Then status 201

    Given path '/api/v1/warehouses', warehouseId, 'inventory', 'restock'
    And headers managerHeaders
    And request restockPayload
    When method post
    Then status 200
    And match response == '#[]'
    And match each response == inventoryItemContract

    * def sku1 = karate.filter(response, function(x){ return x.sku == 'SKU-1' })[0]
    * def sku2 = karate.filter(response, function(x){ return x.sku == 'SKU-2' })[0]
    And match sku1.quantity == 10
    And match sku1.reserved == 0
    And match sku1.available == 10
    And match sku2.quantity == 5
    And match sku2.reserved == 0
    And match sku2.available == 5

    Given path '/api/v1/warehouses', warehouseId, 'inventory'
    And headers managerHeaders
    When method get
    Then status 200
    And match response == '#[]'
    And match each response == inventoryItemContract

    * def queriedSku1 = karate.filter(response, function(x){ return x.sku == 'SKU-1' })[0]
    * def queriedSku2 = karate.filter(response, function(x){ return x.sku == 'SKU-2' })[0]
    And match queriedSku1.quantity == 10
    And match queriedSku1.available == 10
    And match queriedSku2.quantity == 5
    And match queriedSku2.available == 5
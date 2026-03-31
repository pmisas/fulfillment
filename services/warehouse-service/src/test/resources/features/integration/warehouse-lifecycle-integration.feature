@integration
Feature: warehouse lifecycle integration

  Background:
    * url baseUrl
    * def admin = adminHeaders
    * def createWarehousePayload =
    """
    {
      "city": "Bogota Centro",
      "lat": 4.7110,
      "lng": -74.0721
    }
    """

  Scenario: admin creates a warehouse, gets it by id and lists warehouses
    Given path '/api/v1/warehouses'
    And headers admin
    And request createWarehousePayload
    When method post
    Then status 201
    And match response ==
    """
    {
      warehouseId: '#string',
      city: 'bogota centro',
      lat: 4.7110,
      lng: -74.0721
    }
    """
    * def warehouseId = response.warehouseId

    Given path '/api/v1/warehouses', warehouseId
    And headers admin
    When method get
    Then status 200
    And match response.warehouseId == warehouseId
    And match response.city == 'bogota centro'

    Given path '/api/v1/warehouses'
    And headers admin
    When method get
    Then status 200
    And match response == '#[]'
    And match each response contains { warehouseId: '#string', city: '#string', lat: '#number', lng: '#number' }
    And match response[*].warehouseId contains warehouseId

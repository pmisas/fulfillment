@integration
Feature: warehouse manager administration integration

  Background:
    * url baseUrl
    * def admin = adminHeaders

    Given path '/api/v1/warehouses'
    And headers admin
    And request { city: 'Barranquilla', lat: 10.9685, lng: -74.7813 }
    When method post
    Then status 201
    * def warehouseId = response.warehouseId

  Scenario: admin assigns, lists, removes and lists managers again
    Given path '/api/v1/warehouses', warehouseId, 'managers'
    And headers admin
    And request { userId: 'manager-1' }
    When method post
    Then status 201
    And match response ==
    """
    {
      userId: 'manager-1',
      warehouseId: '#(warehouseId)',
      active: true,
      assignedAt: '#string',
      assignedBy: 'admin-1',
      updatedAt: '#string'
    }
    """

    Given path '/api/v1/warehouses', warehouseId, 'managers'
    And headers admin
    When method get
    Then status 200
    And match response.warehouseId == warehouseId
    And match response.managerUserIds contains 'manager-1'

    Given path '/api/v1/warehouses', warehouseId, 'managers', 'manager-1'
    And headers admin
    When method delete
    Then status 200
    And match response.userId == 'manager-1'
    And match response.warehouseId == warehouseId
    And match response.active == false

    Given path '/api/v1/warehouses', warehouseId, 'managers'
    And headers admin
    When method get
    Then status 200
    And match response.warehouseId == warehouseId
    And match response.managerUserIds !contains 'manager-1'

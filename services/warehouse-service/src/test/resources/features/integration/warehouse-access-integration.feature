@integration
Feature: warehouse access integration

  Background:
    * url baseUrl
    * def admin = adminHeaders
    * def manager = managerHeaders
    * def otherManager = otherManagerHeaders

    Given path '/api/v1/warehouses'
    And headers admin
    And request { city: 'Medellin', lat: 6.2442, lng: -75.5812 }
    When method post
    Then status 201
    * def warehouseId = response.warehouseId

    Given path '/api/v1/warehouses', warehouseId, 'managers'
    And headers admin
    And request { userId: 'manager-1' }
    When method post
    Then status 201

  Scenario: an assigned manager can get their warehouse
    Given path '/api/v1/warehouses', warehouseId
    And headers manager
    When method get
    Then status 200
    And match response.warehouseId == warehouseId
    And match response.city == 'medellin'

  Scenario: a manager without assignment cannot get the warehouse
    Given path '/api/v1/warehouses', warehouseId
    And headers otherManager
    When method get
    Then status 403
    And match response.error == 'FORBIDDEN'

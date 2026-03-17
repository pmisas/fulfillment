@ignore
Feature: helper que crea una bodega en warehouse-service y retorna su warehouseId

  # Se usa @ignore para que el runner no lo ejecute directamente.
  # Se invoca con: * def setup = call read('classpath:features/inventory/_warehouse-setup.feature')
  # El warehouseId queda disponible como setup.warehouseId

  Scenario: create warehouse
    Given url warehouseServiceUrl
    And path '/api/v1/warehouses'
    And request
    """
    {
      "city": "Bogotá",
      "lat": 4.7110,
      "lng": -74.0721
    }
    """
    When method post
    Then status 201
    * def warehouseId = response.warehouseId

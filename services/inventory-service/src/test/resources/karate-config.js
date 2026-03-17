function fn() {
  // inventory-service corre en 8082
  // warehouse-service corre en 8081 (necesario para validar que la bodega exista)
  var config = {
    baseUrl: 'http://localhost:8082',
    warehouseServiceUrl: 'http://localhost:8081'
  };

  var env = karate.env;
  if (env == 'local') {
    config.baseUrl = 'http://localhost:8082';
    config.warehouseServiceUrl = 'http://localhost:8081';
  }

  return config;
}

function fn() {
  var config = {
    baseUrl: 'http://localhost:8081'
  };

  config.adminHeaders = {
    'X-Test-User': 'admin-1',
    'X-Test-Roles': 'ADMIN'
  };

  config.managerHeaders = {
    'X-Test-User': 'manager-1',
    'X-Test-Roles': 'WAREHOUSE_MANAGER'
  };

  config.otherManagerHeaders = {
    'X-Test-User': 'manager-2',
    'X-Test-Roles': 'WAREHOUSE_MANAGER'
  };

  config.operatorHeaders = {
    'X-Test-User': 'operator-1',
    'X-Test-Roles': 'OPERATOR'
  };

  return config;
}

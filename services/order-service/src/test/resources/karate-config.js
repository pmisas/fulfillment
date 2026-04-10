function fn() {
  var env = karate.env || 'local';

  var config = {
    env: env,
    baseUrl: karate.properties['baseUrl'] || 'http://localhost:8080',
    operatorAToken: karate.properties['operatorAToken'],
    adminToken: karate.properties['adminToken']
  };

  if (env == 'aws') {
    config.baseUrl = karate.properties['baseUrl'] || 'https://ijo9nrul2c.execute-api.us-east-1.amazonaws.com';
  }

  return config;
}
function fn() {
  var config = {
    baseUrl: 'http://localhost:8080'
  };

  var env = karate.env;
  if (env == 'local') {
    config.baseUrl = 'http://localhost:8080';
  }
  if (env == 'aws') {
    config.baseUrl = 'https://tu-api-en-aws.com';
  }

  return config;
}

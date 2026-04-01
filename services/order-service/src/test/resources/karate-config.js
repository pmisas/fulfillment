function fn() {
  var env = karate.env || 'local';

  var config = {
    env: env,
    baseUrl: 'http://localhost:8080'
  };

  if (env == 'aws') {
    config.baseUrl = 'https://tu-api-en-aws.com';
  }

  return config;
}

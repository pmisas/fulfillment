package com.fulfillment.notificationlambda.infrastructure.config;

public record EnvConfig(
    String awsRegion,
    String ordersTable,
    String cognitoUserPoolId,
    String sesFromEmail) {

    public static EnvConfig fromEnvironment() {
        return new EnvConfig(
            env("AWS_REGION", "us-east-1"),
            env("ORDERS_TABLE", "Orders"),
            requireEnv("COGNITO_USER_POOL_ID"),
            requireEnv("SES_FROM_EMAIL")
        );
    }

    private static String env(String key, String defaultValue) {
        String val = System.getenv(key);
        return (val == null || val.isBlank()) ? defaultValue : val;
    }

    private static String requireEnv(String key) {
        String val = System.getenv(key);
        if (val == null || val.isBlank())
            throw new IllegalStateException("Missing required environment variable: " + key);
        return val;
    }
}

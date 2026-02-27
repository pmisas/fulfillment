package com.fulfillment.orderstateprocesor.infrastructure.repository.dynamodb;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

@Configuration
public class DynamoDbConfig {

    @Bean
    DynamoDbAsyncClient dynamoDbAsyncClient(@Value("${aws.region}") String region) {
        return DynamoDbAsyncClient.builder()
            .region(Region.of(region))
            .build();
    }

    @Bean
    DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient(DynamoDbAsyncClient asyncClient) {
        return DynamoDbEnhancedAsyncClient.builder()
            .dynamoDbClient(asyncClient)
            .build();
    }
}

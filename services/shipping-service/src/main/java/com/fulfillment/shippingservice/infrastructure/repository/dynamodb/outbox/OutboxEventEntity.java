package com.fulfillment.shippingservice.infrastructure.repository.dynamodb.outbox;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Getter
@Setter
@NoArgsConstructor
@DynamoDbBean
public class OutboxEventEntity {

    private String eventId;
    private String publishStatus;
    private Long createdAt;
    private Long publishedAt;
    private Integer attempts;
    private String lastError;

    private String aggregateType;
    private String aggregateId;
    private String eventType;
    private String payload;

    private Long ttl;

    @DynamoDbPartitionKey
    public String getEventId() {
        return eventId;
    }
}

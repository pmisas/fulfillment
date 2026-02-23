package com.fulfillment.orderservice.infrastructure.repository.dynamodb.outbox;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

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

    public void setEventId(String eventId) { 
        this.eventId = eventId; 
    }

    public String getPublishStatus() { 
        return publishStatus; 
    }

    public void setPublishStatus(String publishStatus) { 
        this.publishStatus = publishStatus; 
    }

    public Long getCreatedAt() { 
        return createdAt; 
    }

    public void setCreatedAt(Long createdAt) { 
        this.createdAt = createdAt; 
    }

    public Long getPublishedAt() { 
        return publishedAt; 
    }

    public void setPublishedAt(Long publishedAt) { 
        this.publishedAt = publishedAt; 
    }

    public Integer getAttempts() { 
        return attempts; 
    }

    public void setAttempts(Integer attempts) { 
        this.attempts = attempts; 
    }

    public String getLastError() { 
        return lastError; 
    }

    public void setLastError(String lastError) { 
        this.lastError = lastError; 
    }

    public String getAggregateType() { 
        return aggregateType; 
    }

    public void setAggregateType(String aggregateType) { 
        this.aggregateType = aggregateType; 
    }

    public String getAggregateId() { 
        return aggregateId; 
    }

    public void setAggregateId(String aggregateId) { 
        this.aggregateId = aggregateId; 
    }

    public String getEventType() { 
        return eventType; 
    }

    public void setEventType(String eventType) { 
        this.eventType = eventType; 
    }

    public String getPayload() { 
        return payload; 
    }

    public void setPayload(String payload) { 
        this.payload = payload; 
    }

    public Long getTtl() { 
        return ttl; 
    }

    public void setTtl(Long ttl) { 
        this.ttl = ttl; 
    }
}

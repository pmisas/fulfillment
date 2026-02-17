package com.fulfillment.orderservice.infrastructure.idempotency.redis;

import java.time.Duration;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.fulfillment.orderservice.domain.port.IdempotencyStore;

@Component
@Profile("cloud")
public class RedisIdempotencyStore implements IdempotencyStore {

    private final StringRedisTemplate redis;

    public RedisIdempotencyStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Optional<String> getOrderId(String key) {
        String v = redis.opsForValue().get(redisKey(key));
        return Optional.ofNullable(v);
    }

    @Override
    public boolean putIfAbsent(String key, String orderId, Duration ttl) {
        Boolean ok = redis.opsForValue().setIfAbsent(redisKey(key), orderId, ttl);
        return Boolean.TRUE.equals(ok);
    }

    private String redisKey(String key) {
        return "idemp:orders:" + key;
    }

}

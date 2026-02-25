package com.fulfillment.orderservice.infrastructure.idempotency.redis;

import java.time.Duration;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.fulfillment.orderservice.domain.ports.IdempotencyStore;

@Component
@Profile("cloud")
public class RedisIdempotencyStore implements IdempotencyStore {

    private final StringRedisTemplate redis;

    public RedisIdempotencyStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(redis.opsForValue().get(redisKey(key)));
    }

    @Override
    public boolean claimPending(String key, String token, Duration ttl) {
        String value = pendingValue(token);
        Boolean ok = redis.opsForValue().setIfAbsent(redisKey(key), value, ttl);
        return Boolean.TRUE.equals(ok);
    }

    @Override
    public boolean finalizeOrderId(String key, String token, String orderId, Duration ttl) {
        String script = """
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
            else
                return 0
            end
        """;
        String k = redisKey(key);
        String expected = pendingValue(token);
        String ttlMs = String.valueOf(ttl.toMillis());

        var res = redis.execute(
            new org.springframework.data.redis.core.script.DefaultRedisScript<>(script, Object.class),
            java.util.List.of(k),
            expected, orderId, ttlMs
        );
        return res != null && !"0".equals(res.toString());
    }

    @Override
    public boolean release(String key, String token) {
        String script = """
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            else
                return 0
            end
        """;
        String k = redisKey(key);
        String expected = pendingValue(token);

        var res = redis.execute(
            new org.springframework.data.redis.core.script.DefaultRedisScript<>(script, Long.class),
            java.util.List.of(k),
            expected
        );
        return res != null && ((Long) res) > 0;
    }

    
    private String pendingValue(String token) {
        return "PENDING:" + token;
    }

    private String redisKey(String key) {
        return "idemp:orders:" + key;
    }

}

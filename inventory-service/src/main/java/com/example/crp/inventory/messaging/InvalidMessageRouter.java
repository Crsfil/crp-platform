package com.example.crp.inventory.messaging;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Component
public class InvalidMessageRouter {
    private static final String INVALID_KEY_SET = "kafka:invalid:keys";
    private final StringRedisTemplate redis;
    private final KafkaTemplate<String, Object> kafka;

    public InvalidMessageRouter(StringRedisTemplate redis, KafkaTemplate<String, Object> kafka) {
        this.redis = redis;
        this.kafka = kafka;
    }

    public boolean isMarkedInvalid(String key) {
        return key != null && Boolean.TRUE.equals(redis.opsForSet().isMember(INVALID_KEY_SET, key));
    }

    public void routeInvalid(String invalidTopic, String key, Object payload, String reason) {
        if (key != null) {
            redis.opsForSet().add(INVALID_KEY_SET, key);
            redis.expire(INVALID_KEY_SET, Duration.ofHours(12));
        }
        kafka.send(invalidTopic, key, Map.of("reason", reason, "payload", payload));
    }
}

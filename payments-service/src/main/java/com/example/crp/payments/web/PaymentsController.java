package com.example.crp.payments.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentsController {
    private final StringRedisTemplate redis;
    private final KafkaTemplate<String,Object> kafka;
    private final String idempTtl;

    public PaymentsController(StringRedisTemplate redis, KafkaTemplate<String, Object> kafka,
                              @Value("${payments.idempotency-ttl:PT24H}") String idempTtl) {
        this.redis = redis; this.kafka = kafka; this.idempTtl = idempTtl;
    }

    @PostMapping("/webhook")
    public Map<String,Object> webhook(@RequestBody Map<String,Object> payload){
        String eventId = String.valueOf(payload.getOrDefault("eventId", payload.hashCode()));
        String key = "payment:event:"+eventId;
        Boolean set = redis.opsForValue().setIfAbsent(key, "1", Duration.parse(idempTtl));
        if (Boolean.FALSE.equals(set)) {
            return Map.of("status","duplicate");
        }
        // Extract main fields
        Object invoiceId = payload.get("invoiceId");
        Object amount = payload.get("amount");
        kafka.send("payment.received", Map.of("invoiceId", invoiceId, "amount", amount, "eventId", eventId));
        return Map.of("status","accepted");
    }
}

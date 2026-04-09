package com.analytics.stockservice.idempotency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class IdempotencyService {

    private static final Logger LOG = LoggerFactory.getLogger(IdempotencyService.class);
    private static final long TTL_HOURS = 24;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public boolean isAlreadyProcessed(String service, String orderId, String eventType) {
        String key = service + ":" + eventType + ":" + orderId;
        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            LOG.warn("Duplicate event detected - skipping: {}", key);
            return true;
        }
        return false;
    }

    public void markAsProcessed(String service, String orderId, String eventType) {
        String key = service + ":" + eventType + ":" + orderId;
        redisTemplate.opsForValue().set(key, "processed", TTL_HOURS, TimeUnit.HOURS);
        LOG.info("Marked as processed: {}", key);
    }
}
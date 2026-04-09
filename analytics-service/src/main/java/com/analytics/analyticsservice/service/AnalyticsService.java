package com.analytics.analyticsservice.service;

import com.analytics.analyticsservice.dto.AnalyticsSummary;
import com.analytics.analyticsservice.entity.EventLog;
import com.analytics.analyticsservice.repository.EventLogRepository;
import com.analytics.basedomain.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class AnalyticsService {

    private static final Logger LOG = LoggerFactory.getLogger(AnalyticsService.class);
    private static final String SUMMARY_CACHE_KEY = "analytics:summary";

    @Autowired
    private EventLogRepository eventLogRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void logEvent(Order order, String eventType) {
        EventLog log = new EventLog();
        log.setOrderId(order.getOrderId());
        log.setCustomerId(order.getCustomerId());
        log.setProductId(order.getProductId());
        log.setQuantity(order.getQuantity());
        log.setPrice(order.getPrice());
        log.setEventType(eventType);
        log.setEventTime(LocalDateTime.now());
        eventLogRepository.save(log);
        LOG.info("Event logged: {} for order: {}", eventType, order.getOrderId());
        redisTemplate.delete(SUMMARY_CACHE_KEY);
    }

    public AnalyticsSummary getSummary() {
        AnalyticsSummary cached = (AnalyticsSummary) redisTemplate.opsForValue().get(SUMMARY_CACHE_KEY);
        if (cached != null) {
            LOG.info("Returning analytics summary from Redis cache");
            return cached;
        }

        LOG.info("Cache miss - fetching analytics summary from PostgreSQL");
        AnalyticsSummary summary = new AnalyticsSummary();
        summary.setTotalOrders(eventLogRepository.count());
        summary.setConfirmedOrders(eventLogRepository.countByEventType("CONFIRMED"));
        summary.setCancelledOrders(eventLogRepository.countByEventType("CANCELLED"));
        summary.setStockFailedOrders(eventLogRepository.countByEventType("STOCK_FAILED"));
        Double revenue = eventLogRepository.getTotalRevenue();
        summary.setTotalRevenue(revenue != null ? revenue : 0.0);

        redisTemplate.opsForValue().set(SUMMARY_CACHE_KEY, summary, 30, TimeUnit.SECONDS);
        LOG.info("Analytics summary cached in Redis for 30 seconds");
        return summary;
    }
}
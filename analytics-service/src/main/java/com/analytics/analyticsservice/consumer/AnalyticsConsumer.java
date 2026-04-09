package com.analytics.analyticsservice.consumer;

import com.analytics.analyticsservice.service.AnalyticsService;
import com.analytics.basedomain.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(AnalyticsConsumer.class);

    @Autowired
    private AnalyticsService analyticsService;

    @KafkaListener(topics = "orders", groupId = "analytics-group")
    public void consumeOrder(Order order) {
        LOG.info("Analytics received NEW order: {}", order.getOrderId());
        analyticsService.logEvent(order, "NEW");
    }

    @KafkaListener(topics = "payment-success", groupId = "analytics-group")
    public void consumePaymentSuccess(Order order) {
        LOG.info("Analytics received CONFIRMED order: {}", order.getOrderId());
        analyticsService.logEvent(order, "CONFIRMED");
    }

    @KafkaListener(topics = "payment-failed", groupId = "analytics-group")
    public void consumePaymentFailed(Order order) {
        LOG.info("Analytics received CANCELLED order: {}", order.getOrderId());
        analyticsService.logEvent(order, "CANCELLED");
    }

    @KafkaListener(topics = "stock-failed", groupId = "analytics-group")
    public void consumeStockFailed(Order order) {
        LOG.info("Analytics received STOCK_FAILED order: {}", order.getOrderId());
        analyticsService.logEvent(order, "STOCK_FAILED");
    }
}
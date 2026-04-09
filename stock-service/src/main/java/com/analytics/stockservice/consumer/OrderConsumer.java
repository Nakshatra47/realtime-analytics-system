package com.analytics.stockservice.consumer;

import com.analytics.basedomain.domain.Order;
import com.analytics.stockservice.idempotency.IdempotencyService;
import com.analytics.stockservice.service.StockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class OrderConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(OrderConsumer.class);
    private static final String SERVICE_NAME = "stock-service";

    @Autowired
    private StockService stockService;

    @Autowired
    private IdempotencyService idempotencyService;

    @KafkaListener(topics = "orders", groupId = "stock-group")
    public void consumeOrder(Order order) {
        if (idempotencyService.isAlreadyProcessed(SERVICE_NAME, order.getOrderId(), "ORDER")) {
            return;
        }
        LOG.info("Stock-service received order: {}", order.getOrderId());
        stockService.processOrder(order);
        idempotencyService.markAsProcessed(SERVICE_NAME, order.getOrderId(), "ORDER");
    }

    @KafkaListener(topics = "payment-failed", groupId = "stock-group")
    public void consumePaymentFailed(Order order) {
        if (idempotencyService.isAlreadyProcessed(SERVICE_NAME, order.getOrderId(), "PAYMENT_FAILED")) {
            return;
        }
        LOG.info("Stock-service received payment failure - releasing stock for order: {}", order.getOrderId());
        stockService.releaseStock(order);
        idempotencyService.markAsProcessed(SERVICE_NAME, order.getOrderId(), "PAYMENT_FAILED");
    }

    @KafkaListener(topics = "payment-success", groupId = "stock-group")
    public void consumePaymentSuccess(Order order) {
        if (idempotencyService.isAlreadyProcessed(SERVICE_NAME, order.getOrderId(), "PAYMENT_SUCCESS")) {
            return;
        }
        LOG.info("Stock-service received payment success - confirming stock for order: {}", order.getOrderId());
        stockService.confirmStock(order);
        idempotencyService.markAsProcessed(SERVICE_NAME, order.getOrderId(), "PAYMENT_SUCCESS");
    }
}
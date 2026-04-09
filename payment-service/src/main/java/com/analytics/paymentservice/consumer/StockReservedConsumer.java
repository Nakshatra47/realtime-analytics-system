package com.analytics.paymentservice.consumer;

import com.analytics.basedomain.domain.Order;
import com.analytics.paymentservice.idempotency.IdempotencyService;
import com.analytics.paymentservice.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class StockReservedConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(StockReservedConsumer.class);
    private static final String SERVICE_NAME = "payment-service";

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private IdempotencyService idempotencyService;

    @KafkaListener(topics = "stock-reserved", groupId = "payment-group")
    public void consumeStockReserved(Order order) {
        if (idempotencyService.isAlreadyProcessed(SERVICE_NAME, order.getOrderId(), "STOCK_RESERVED")) {
            return;
        }
        LOG.info("Payment-service received STOCK_RESERVED for order: {}", order.getOrderId());
        paymentService.processPayment(order);
        idempotencyService.markAsProcessed(SERVICE_NAME, order.getOrderId(), "STOCK_RESERVED");
    }
}
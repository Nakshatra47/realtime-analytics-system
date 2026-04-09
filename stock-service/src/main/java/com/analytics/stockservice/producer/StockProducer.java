package com.analytics.stockservice.producer;

import com.analytics.basedomain.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class StockProducer {

    private static final Logger LOG = LoggerFactory.getLogger(StockProducer.class);

    @Autowired
    private KafkaTemplate<String, Order> kafkaTemplate;

    public void sendStockReserved(Order order) {
        LOG.info("Publishing STOCK_RESERVED for order: {}", order.getOrderId());
        kafkaTemplate.send("stock-reserved", order.getOrderId(), order);
    }

    public void sendStockFailed(Order order) {
        LOG.info("Publishing STOCK_FAILED for order: {}", order.getOrderId());
        kafkaTemplate.send("stock-failed", order.getOrderId(), order);
    }
}
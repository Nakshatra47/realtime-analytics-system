package com.analytics.orderservice.producer;

import com.analytics.basedomain.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderProducer {

    private static final Logger LOG = LoggerFactory.getLogger(OrderProducer.class);
    private static final String TOPIC = "orders";

    @Autowired
    private KafkaTemplate<String, Order> kafkaTemplate;

    public void sendOrder(Order order) {
        LOG.info("Publishing order to Kafka: {} - status: {}", order.getOrderId(), order.getStatus());
        kafkaTemplate.send(TOPIC, order.getOrderId(), order);
    }
}
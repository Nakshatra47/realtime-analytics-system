package com.analytics.paymentservice.producer;

import com.analytics.basedomain.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PaymentProducer {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentProducer.class);

    @Autowired
    private KafkaTemplate<String, Order> kafkaTemplate;

    public void sendPaymentSuccess(Order order) {
        LOG.info("Publishing PAYMENT_SUCCESS for order: {}", order.getOrderId());
        kafkaTemplate.send("payment-success", order.getOrderId(), order);
    }

    public void sendPaymentFailed(Order order) {
        LOG.info("Publishing PAYMENT_FAILED for order: {}", order.getOrderId());
        kafkaTemplate.send("payment-failed", order.getOrderId(), order);
    }
}
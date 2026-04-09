package com.analytics.paymentservice.service;

import com.analytics.basedomain.domain.Order;
import com.analytics.basedomain.domain.OrderStatus;
import com.analytics.paymentservice.producer.PaymentProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private PaymentProducer paymentProducer;

    @Value("${payment.processing.delay-ms:0}")
    private long delayMs;

    public void processPayment(Order order) {
        LOG.info("Processing payment for order: {} amount: {}", order.getOrderId(), order.getPrice());

        if (delayMs > 0) {
            try {
                LOG.info("Simulating payment processing delay of {}ms...", delayMs);
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        boolean paymentSuccess = order.getPrice() < 10000.0;

        if (paymentSuccess) {
            order.setStatus(OrderStatus.CONFIRMED);
            LOG.info("Payment SUCCESS for order: {}", order.getOrderId());
            paymentProducer.sendPaymentSuccess(order);
        } else {
            order.setStatus(OrderStatus.CANCELLED);
            LOG.info("Payment FAILED for order: {} - amount too high", order.getOrderId());
            paymentProducer.sendPaymentFailed(order);
        }
    }
}
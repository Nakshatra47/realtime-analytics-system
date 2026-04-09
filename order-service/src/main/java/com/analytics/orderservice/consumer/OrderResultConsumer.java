package com.analytics.orderservice.consumer;

import com.analytics.basedomain.domain.Order;
import com.analytics.basedomain.domain.OrderStatus;
import com.analytics.orderservice.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class OrderResultConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(OrderResultConsumer.class);

    @Autowired
    private OrderService orderService;

    @KafkaListener(topics = "payment-success", groupId = "order-group")
    public void handlePaymentSuccess(Order order) {
        LOG.info("Order CONFIRMED: {}", order.getOrderId());
        orderService.updateOrderStatus(order.getOrderId(), OrderStatus.CONFIRMED);
    }

    @KafkaListener(topics = "payment-failed", groupId = "order-group")
    public void handlePaymentFailed(Order order) {
        LOG.info("Order CANCELLED due to payment failure: {}", order.getOrderId());
        orderService.updateOrderStatus(order.getOrderId(), OrderStatus.CANCELLED);
    }

    @KafkaListener(topics = "stock-failed", groupId = "order-group")
    public void handleStockFailed(Order order) {
        LOG.info("Order CANCELLED due to stock failure: {}", order.getOrderId());
        orderService.updateOrderStatus(order.getOrderId(), OrderStatus.CANCELLED);
    }
}
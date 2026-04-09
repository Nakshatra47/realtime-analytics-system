package com.analytics.orderservice.service;

import com.analytics.basedomain.domain.Order;
import com.analytics.basedomain.domain.OrderStatus;
import com.analytics.orderservice.entity.OrderEntity;
import com.analytics.orderservice.producer.OrderProducer;
import com.analytics.orderservice.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class OrderService {

    private static final Logger LOG = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderProducer orderProducer;

    public Order createOrder(Order request) {
        Order order = Order.create(
            request.getCustomerId(),
            request.getProductId(),
            request.getQuantity(),
            request.getPrice()
        );

        OrderEntity entity = mapToEntity(order);
        orderRepository.save(entity);
        LOG.info("Order saved to DB: {} - status: {}", order.getOrderId(), order.getStatus());

        orderProducer.sendOrder(order);
        return order;
    }

    public void updateOrderStatus(String orderId, OrderStatus status) {
        orderRepository.findById(orderId).ifPresent(entity -> {
            entity.setStatus(status);
            entity.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(entity);
            LOG.info("Order status updated: {} - {}", orderId, status);
        });
    }

    private OrderEntity mapToEntity(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.setOrderId(order.getOrderId());
        entity.setCustomerId(order.getCustomerId());
        entity.setProductId(order.getProductId());
        entity.setQuantity(order.getQuantity());
        entity.setPrice(order.getPrice());
        entity.setStatus(order.getStatus());
        entity.setCreatedAt(order.getCreatedAt());
        entity.setUpdatedAt(order.getUpdatedAt());
        return entity;
    }
}
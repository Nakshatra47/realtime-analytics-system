package com.analytics.orderservice.controller;

import com.analytics.basedomain.domain.Order;
import com.analytics.orderservice.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order request) {
        Order order = orderService.createOrder(request);
        return ResponseEntity.ok(order);
    }
}
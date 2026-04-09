package com.analytics.basedomain.domain;

public enum OrderStatus {
    NEW,
    STOCK_CHECKING,
    STOCK_RESERVED,
    STOCK_FAILED,
    PAYMENT_PROCESSING,
    CONFIRMED,
    CANCELLED
}
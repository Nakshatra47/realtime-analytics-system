package com.analytics.stockservice.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class DeadLetterConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(DeadLetterConsumer.class);

    @KafkaListener(topics = "orders-dlt", groupId = "stock-dlt-group")
    public void handleDeadLetter(ConsumerRecord<String, Object> record) {
        LOG.error("DEAD LETTER received!");
        LOG.error("Original topic: orders");
        LOG.error("Failed message key: {}", record.key());
        LOG.error("Failed message value: {}", record.value());
        LOG.error("Action required: Manual intervention needed for this message");
    }
}
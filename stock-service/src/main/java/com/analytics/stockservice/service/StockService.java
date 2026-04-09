package com.analytics.stockservice.service;

import com.analytics.basedomain.domain.Order;
import com.analytics.basedomain.domain.OrderStatus;
import com.analytics.stockservice.entity.StockEntity;
import com.analytics.stockservice.producer.StockProducer;
import com.analytics.stockservice.repository.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class StockService {

    private static final Logger LOG = LoggerFactory.getLogger(StockService.class);

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockProducer stockProducer;

    public void processOrder(Order order) {
        LOG.info("Checking stock for order: {} product: {}", order.getOrderId(), order.getProductId());

        Optional<StockEntity> stockOpt = stockRepository.findByProductId(order.getProductId());

        if (stockOpt.isPresent()) {
            StockEntity stock = stockOpt.get();
            if (stock.getAvailableQuantity() >= order.getQuantity()) {
                stock.setAvailableQuantity(stock.getAvailableQuantity() - order.getQuantity());
                stock.setReservedQuantity(stock.getReservedQuantity() + order.getQuantity());
                stockRepository.save(stock);
                order.setStatus(OrderStatus.STOCK_RESERVED);
                LOG.info("Stock reserved for order: {} - available: {} reserved: {}",
                    order.getOrderId(), stock.getAvailableQuantity(), stock.getReservedQuantity());
                stockProducer.sendStockReserved(order);
            } else {
                order.setStatus(OrderStatus.STOCK_FAILED);
                LOG.info("Insufficient stock for order: {} - available: {} required: {}",
                    order.getOrderId(), stock.getAvailableQuantity(), order.getQuantity());
                stockProducer.sendStockFailed(order);
            }
        } else {
            StockEntity newStock = new StockEntity();
            newStock.setProductId(order.getProductId());
            newStock.setAvailableQuantity(100 - order.getQuantity());
            newStock.setReservedQuantity(order.getQuantity());
            stockRepository.save(newStock);
            order.setStatus(OrderStatus.STOCK_RESERVED);
            LOG.info("New stock created and reserved for order: {} - available: {} reserved: {}",
                order.getOrderId(), 100 - order.getQuantity(), order.getQuantity());
            stockProducer.sendStockReserved(order);
        }
    }

    public void releaseStock(Order order) {
        LOG.info("Releasing stock reservation for order: {}", order.getOrderId());
        stockRepository.findByProductId(order.getProductId()).ifPresent(stock -> {
            stock.setAvailableQuantity(stock.getAvailableQuantity() + order.getQuantity());
            stock.setReservedQuantity(stock.getReservedQuantity() - order.getQuantity());
            stockRepository.save(stock);
            LOG.info("Stock released for product: {} - available: {} reserved: {}",
                order.getProductId(), stock.getAvailableQuantity(), stock.getReservedQuantity());
        });
    }

    public void confirmStock(Order order) {
        LOG.info("Confirming stock sale for order: {}", order.getOrderId());
        stockRepository.findByProductId(order.getProductId()).ifPresent(stock -> {
            stock.setReservedQuantity(stock.getReservedQuantity() - order.getQuantity());
            stockRepository.save(stock);
            LOG.info("Stock confirmed sold for product: {} - available: {} reserved: {}",
                order.getProductId(), stock.getAvailableQuantity(), stock.getReservedQuantity());
        });
    }
}
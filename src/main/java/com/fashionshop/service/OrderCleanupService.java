package com.fashionshop.service;

import com.fashionshop.entity.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Auto-cancels PENDING_PAYMENT orders that are older than 10 minutes.
 * This handles cases where user closes browser before QR code expires.
 */
@Service
public class OrderCleanupService {

    private static final Logger log = LoggerFactory.getLogger(OrderCleanupService.class);
    private final OrderService orderService;

    public OrderCleanupService(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Runs every 5 minutes — cancels PENDING_PAYMENT orders older than 10 minutes.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    @Transactional
    public void cleanupStaleOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);

        List<Order> allOrders = orderService.findAll();
        int cancelled = 0;

        for (Order order : allOrders) {
            if (order.getStatus() == Order.OrderStatus.PENDING_PAYMENT
                    && order.getCreatedAt() != null
                    && order.getCreatedAt().isBefore(cutoff)) {
                order.setStatus(Order.OrderStatus.CANCELLED);
                orderService.save(order);
                cancelled++;
                log.info("⏰ Auto-cancelled stale order #{} (created at {})", order.getId(), order.getCreatedAt());
            }
        }

        if (cancelled > 0) {
            log.info("🧹 Cleanup complete: {} stale orders cancelled", cancelled);
        }
    }
}

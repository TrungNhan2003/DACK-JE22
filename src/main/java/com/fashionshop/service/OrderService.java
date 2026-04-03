package com.fashionshop.service;

import com.fashionshop.dto.CheckoutDTO;
import com.fashionshop.entity.Order;
import com.fashionshop.entity.User;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface OrderService {

    Order createOrder(CheckoutDTO dto, String userEmail);

    Optional<Order> findById(Long id);

    List<Order> findByUserEmail(String email);

    Optional<User> findByEmail(String email);

    List<Order> findAll();

    void updateStatus(Long id, Order.OrderStatus status);

    Long countTodayOrders();

    BigDecimal revenueThisMonth();

    List<Order> findTop10Latest();
}

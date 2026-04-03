package com.fashionshop.service;

import com.fashionshop.dto.CheckoutDTO;
import com.fashionshop.entity.*;
import com.fashionshop.repository.OrderItemRepository;
import com.fashionshop.repository.OrderRepository;
import com.fashionshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final OrderItemRepository orderItemRepository;

    @Override
    @Transactional
    public Order createOrder(CheckoutDTO dto, String userEmail) {
        User user = null;
        if (userEmail != null) {
            user = userRepository.findByEmail(userEmail).orElse(null);
        }

        BigDecimal totalAmount = cartService.getCartTotal();

        // Map payment method from DTO (string) to Order.PaymentMethod enum
        Order.PaymentMethod paymentMethod = Order.PaymentMethod.COD;
        if (dto.getPaymentMethod() != null) {
            switch (dto.getPaymentMethod()) {
                case "BANK_TRANSFER" -> paymentMethod = Order.PaymentMethod.BANK_TRANSFER;
                case "MOMO" -> paymentMethod = Order.PaymentMethod.MOMO;
                default -> paymentMethod = Order.PaymentMethod.COD;
            }
        }

        Order order = Order.builder()
                .user(user)
                .fullName(dto.getFullName())
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .totalAmount(totalAmount)
                .status(Order.OrderStatus.PENDING)
                .paymentMethod(paymentMethod)
                .build();

        Order savedOrder = orderRepository.save(order);

        List<OrderItem> orderItems = cartService.getCartItems().stream()
                .map(cartItem -> OrderItem.builder()
                        .order(savedOrder)
                        .product(cartItem.getProduct())
                        .quantity(cartItem.getQuantity())
                        .price(cartItem.getPrice())
                        .build())
                .toList();

        orderItemRepository.saveAll(orderItems);

        return savedOrder;
    }

    @Override
    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    @Override
    public List<Order> findByUserEmail(String email) {
        return orderRepository.findByUserEmailOrderByCreatedAtDesc(email);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Order.OrderStatus status) {
        orderRepository.findById(id).ifPresent(order -> {
            order.setStatus(status);
            orderRepository.save(order);
        });
    }

    @Override
    public Long countTodayOrders() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        return orderRepository.countByCreatedAtAfter(startOfDay);
    }

    @Override
    public BigDecimal revenueThisMonth() {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
        return orderRepository.sumTotalAmountByCreatedAtAfter(startOfMonth);
    }

    @Override
    public List<Order> findTop10Latest() {
        return orderRepository.findTop10ByOrderByCreatedAtDesc();
    }
}

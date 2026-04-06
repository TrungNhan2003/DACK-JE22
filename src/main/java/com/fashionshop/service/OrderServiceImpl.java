package com.fashionshop.service;

import com.fashionshop.dto.CheckoutDTO;
import com.fashionshop.entity.CartItem;
import com.fashionshop.entity.Order;
import com.fashionshop.entity.OrderItem;
import com.fashionshop.entity.Product;
import com.fashionshop.entity.User;
import com.fashionshop.repository.OrderItemRepository;
import com.fashionshop.repository.OrderRepository;
import com.fashionshop.repository.ProductRepository;
import com.fashionshop.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final OrderItemRepository orderItemRepository;
    private final VoucherService voucherService;
    private final ProductRepository productRepository;

    public OrderServiceImpl(OrderRepository orderRepository,
                            UserRepository userRepository,
                            CartService cartService,
                            OrderItemRepository orderItemRepository,
                            VoucherService voucherService,
                            ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.cartService = cartService;
        this.orderItemRepository = orderItemRepository;
        this.voucherService = voucherService;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public Order createOrder(CheckoutDTO dto, String userEmail) {
        User user = null;
        if (userEmail != null) {
            user = userRepository.findByEmail(userEmail).orElse(null);
        }

        List<CartItem> cartItems = cartService.getCartItems();
        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalStateException("Giỏ hàng đang trống");
        }

        // Kiểm tra tồn kho trước khi tạo đơn
        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Không tìm thấy sản phẩm ID = " + cartItem.getProduct().getId()));

            int currentStock = product.getStock() != null ? product.getStock() : 0;
            int buyQty = cartItem.getQuantity() != null ? cartItem.getQuantity() : 0;

            if (buyQty <= 0) {
                throw new IllegalArgumentException("Số lượng mua không hợp lệ");
            }

            if (currentStock < buyQty) {
                throw new IllegalArgumentException("Sản phẩm \"" + product.getName() + "\" không đủ tồn kho");
            }
        }

        BigDecimal subtotal = cartService.getCartTotal();
        VoucherService.DiscountResult voucherResult = voucherService.apply(dto.getVoucherCode(), subtotal);
        if (voucherResult.invalidCode()) {
            throw new IllegalArgumentException("invalid_voucher");
        }

        BigDecimal discount = voucherResult.discountAmount() != null
                ? voucherResult.discountAmount()
                : BigDecimal.ZERO;

        BigDecimal totalAmount = subtotal.subtract(discount).max(BigDecimal.ZERO);

        Order.PaymentMethod paymentMethod = Order.PaymentMethod.COD;
        if (dto.getPaymentMethod() != null) {
            switch (dto.getPaymentMethod()) {
                case "BANK_TRANSFER" -> paymentMethod = Order.PaymentMethod.BANK_TRANSFER;
                case "MOMO" -> paymentMethod = Order.PaymentMethod.MOMO;
                default -> paymentMethod = Order.PaymentMethod.COD;
            }
        }

        // Set status based on payment method
        Order.OrderStatus initialStatus = paymentMethod == Order.PaymentMethod.MOMO
                ? Order.OrderStatus.PENDING_PAYMENT
                : Order.OrderStatus.PENDING;

        Order order = new Order();
        order.setUser(user);
        order.setFullName(dto.getFullName());
        order.setPhone(dto.getPhone());
        order.setAddress(dto.getAddress());
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discount);
        order.setVoucherCode(voucherResult.normalizedCode());
        order.setStatus(initialStatus);
        order.setPaymentMethod(paymentMethod);

        Order savedOrder = orderRepository.save(order);

        List<OrderItem> orderItems = cartItems.stream().map(cartItem -> {
            Product product = productRepository.findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Không tìm thấy sản phẩm ID = " + cartItem.getProduct().getId()));

            int currentStock = product.getStock() != null ? product.getStock() : 0;
            int buyQty = cartItem.getQuantity() != null ? cartItem.getQuantity() : 0;

            // Trừ tồn kho
            product.setStock(currentStock - buyQty);
            productRepository.save(product);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(product);
            orderItem.setQuantity(buyQty);
            orderItem.setPrice(cartItem.getPrice());
            orderItem.setSize(cartItem.getSize());
            orderItem.setColor(cartItem.getColor());
            return orderItem;
        }).toList();

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

    @Override
    @Transactional
    public Order save(Order order) {
        return orderRepository.save(order);
    }
}
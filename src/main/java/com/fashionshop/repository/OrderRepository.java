package com.fashionshop.repository;

import com.fashionshop.entity.Order;
import com.fashionshop.entity.Order.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Lấy đơn hàng theo user (lịch sử mua hàng)
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Lấy đơn hàng theo email user
    @Query("SELECT o FROM Order o WHERE o.user.email = :email ORDER BY o.createdAt DESC")
    List<Order> findByUserEmailOrderByCreatedAtDesc(@Param("email") String email);

    // Lọc đơn hàng theo trạng thái (admin)
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    // Đếm đơn hàng hôm nay (dashboard admin)
    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :startOfDay")
    Long countByCreatedAtAfter(@Param("startOfDay") LocalDateTime startOfDay);

    // Tổng doanh thu trong tháng (dashboard admin)
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
           "WHERE o.status = 'DONE' AND o.createdAt >= :startOfMonth")
    BigDecimal sumTotalAmountByCreatedAtAfter(@Param("startOfMonth") LocalDateTime startOfMonth);

    // Đơn hàng gần đây nhất (dashboard admin)
    List<Order> findTop10ByOrderByCreatedAtDesc();
}
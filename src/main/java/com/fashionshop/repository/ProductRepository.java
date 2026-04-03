package com.fashionshop.repository;

import com.fashionshop.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySlug(String slug);

    // Tìm kiếm + lọc theo danh mục + khoảng giá
    @Query("SELECT p FROM Product p WHERE " +
           "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Product> searchProducts(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") java.math.BigDecimal minPrice,
            @Param("maxPrice") java.math.BigDecimal maxPrice,
            Pageable pageable
    );

    // Lấy sản phẩm theo danh mục
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    // Lấy sản phẩm theo danh mục (loại trừ id)
    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.id != :excludeId")
    List<Product> findByCategoryIdAndIdNot(@Param("categoryId") Long categoryId, 
                                           @Param("excludeId") Long excludeId, Pageable pageable);

    // Tìm theo tên (search)
    Page<Product> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

    // Lấy sản phẩm nổi bật (mới nhất)
    List<Product> findTop8ByOrderByCreatedAtDesc();

    // Sản phẩm sắp hết hàng (admin)
    List<Product> findByStockLessThan(int stock);
}
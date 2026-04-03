package com.fashionshop.service;

import com.fashionshop.dto.ProductDTO;
import com.fashionshop.entity.Category;
import com.fashionshop.entity.Product;
import org.springframework.data.domain.Page;
import java.util.List;
import java.util.Optional;

public interface ProductService {

    List<Product> findFeaturedProducts(int limit);

    Product findBySlug(String slug);

    Optional<Product> findById(Long id);

    Page<Product> findAllByCategory(Long categoryId, int page, int size);

    Page<Product> searchByName(String keyword, int page, int size);

    List<Category> findAllCategories();

    List<Product> findRelatedProducts(Category category, Long excludeId, int limit);

    List<Product> findAll();

    List<Product> findLowStock();

    Product save(ProductDTO dto);

    Product update(Long id, ProductDTO dto);

    void delete(Long id);
}

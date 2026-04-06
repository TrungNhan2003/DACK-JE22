package com.fashionshop.service;

import com.fashionshop.dto.ProductDTO;
import com.fashionshop.entity.Category;
import com.fashionshop.entity.Product;
import com.fashionshop.repository.CategoryRepository;
import com.fashionshop.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<Product> findFeaturedProducts(int limit) {
        return productRepository.findTop8ByOrderByCreatedAtDesc();
    }

    @Override
    public Product findBySlug(String slug) {
        return productRepository.findBySlug(slug).orElse(null);
    }

    @Override
    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    @Override
    public Page<Product> findAllByCategory(Long categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (categoryId != null) {
            return productRepository.findByCategoryId(categoryId, pageable);
        }
        return productRepository.findAll(pageable);
    }

    @Override
    public Page<Product> searchByName(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productRepository.findByNameContainingIgnoreCase(keyword, pageable);
    }

    @Override
    public List<Category> findAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public List<Product> findRelatedProducts(Category category, Long excludeId, int limit) {
        if (category != null) {
            return productRepository.findByCategoryIdAndIdNot(category.getId(), excludeId, PageRequest.of(0, limit))
                    .stream().limit(limit).toList();
        }
        return productRepository.findTop8ByOrderByCreatedAtDesc().stream()
                .filter(p -> !p.getId().equals(excludeId))
                .limit(limit)
                .toList();
    }

    @Override
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Override
    public List<Product> findLowStock() {
        return productRepository.findByStockLessThan(5);
    }

    @Override
    @Transactional
    public Product save(ProductDTO dto) {
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));

        Product product = new Product();
        product.setName(dto.getName());
        product.setSlug(generateSlug(dto.getName()));
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setSalePrice(dto.getDiscountPrice());
        product.setStock(dto.getStock());
        product.setImageUrl(dto.getImage());
        product.setCategory(category);

        return productRepository.save(product);
    }

    @Override
    @Transactional
    public Product update(Long id, ProductDTO dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setSalePrice(dto.getDiscountPrice());
        product.setStock(dto.getStock());
        product.setImageUrl(dto.getImage());
        product.setCategory(category);

        return productRepository.save(product);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        productRepository.deleteById(id);
    }

    private String generateSlug(String name) {
        String slug = name.toLowerCase()
                .replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a")
                .replaceAll("[èéẹẻẽêềếệểễ]", "e")
                .replaceAll("[ìíịỉĩ]", "i")
                .replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o")
                .replaceAll("[ùúụủũưừứựửữ]", "u")
                .replaceAll("[ỳýỵỷỹ]", "y")
                .replaceAll("[đ]", "d")
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
        return slug + "-" + System.currentTimeMillis();
    }
}

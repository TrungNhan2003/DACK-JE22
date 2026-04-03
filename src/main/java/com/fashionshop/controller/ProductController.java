package com.fashionshop.controller;

import com.fashionshop.entity.Product;
import com.fashionshop.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("products", productService.findFeaturedProducts(8));
        return "user/home";
    }

    @GetMapping("/products")
    public String products(Model model,
                           @RequestParam(required = false) Long category,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "12") int size) {
        Page<Product> productPage = productService.findAllByCategory(category, page, size);
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("paging", productPage);
        model.addAttribute("categories", productService.findAllCategories());
        model.addAttribute("currentCategory", category);
        return "user/products";
    }

    @GetMapping("/products/{slug}")
    public String productDetail(@PathVariable String slug, Model model) {
        Product product = productService.findBySlug(slug);
        if (product != null) {
            model.addAttribute("product", product);
            model.addAttribute("relatedProducts", productService.findRelatedProducts(product.getCategory(), product.getId(), 4));
            return "user/product-detail";
        }
        return "redirect:/products";
    }

    @GetMapping("/search")
    public String search(Model model,
                         @RequestParam String keyword,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "12") int size) {
        Page<Product> productPage = productService.searchByName(keyword, page, size);
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("paging", productPage);
        model.addAttribute("keyword", keyword);
        return "user/products";
    }
}

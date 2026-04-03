package com.fashionshop.controller.admin;

import com.fashionshop.dto.ProductDTO;
import com.fashionshop.entity.Product;
import com.fashionshop.service.CategoryService;
import com.fashionshop.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/products")
public class AdminProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * Fix binding khi người dùng nhập số có dấu phân tách (vd: 150,000 hoặc 150.000)
     * hoặc để trống các field optional (vd: discountPrice).
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(BigDecimal.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                if (text == null || text.trim().isEmpty()) {
                    setValue(null);
                    return;
                }
                String raw = text.trim();
                // Keep digits, separators and '-' only.
                String cleaned = raw.replaceAll("[^0-9,\\.\\-]", "");
                if (cleaned.isEmpty() || cleaned.equals("-")) {
                    setValue(null);
                    return;
                }

                try {
                    // Money in VND: we want INTEGER value (no decimals).
                    // Handle common user inputs like: 150,000 / 150.000 / 500000000.000
                    int commaIdx = cleaned.lastIndexOf(',');
                    int dotIdx = cleaned.lastIndexOf('.');

                    BigDecimal value;
                    if (commaIdx >= 0 && dotIdx >= 0) {
                        // Both separators exist: last one is decimal separator, the other is grouping.
                        char decimalSep = commaIdx > dotIdx ? ',' : '.';
                        char groupingSep = decimalSep == ',' ? '.' : ',';

                        String noGrouping = cleaned.replace(String.valueOf(groupingSep), "");
                        String normalized = noGrouping.replace(decimalSep, '.');
                        value = new BigDecimal(normalized);
                    } else if (commaIdx >= 0) {
                        // Only comma exists.
                        String before = cleaned.substring(0, commaIdx);
                        String after = cleaned.substring(commaIdx + 1);

                        if (after.length() == 3 && before.replace("-", "").length() <= 3) {
                            // Likely thousands separator: 500,000
                            value = new BigDecimal(before.replace(",", "") + after);
                        } else {
                            // Likely decimal separator: 500000000,000 => drop fractional part
                            value = new BigDecimal(before);
                        }
                    } else if (dotIdx >= 0) {
                        // Only dot exists.
                        String before = cleaned.substring(0, dotIdx);
                        String after = cleaned.substring(dotIdx + 1);

                        if (after.length() == 3 && before.replace("-", "").length() <= 3) {
                            // Likely thousands separator: 500.000
                            value = new BigDecimal(before.replace(".", "") + after);
                        } else {
                            // Likely decimal separator: 500000000.000 => drop fractional part
                            value = new BigDecimal(before);
                        }
                    } else {
                        // No separators
                        value = new BigDecimal(cleaned);
                    }

                    setValue(value.setScale(0, java.math.RoundingMode.DOWN));
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid money: " + text, e);
                }
            }
        });

        binder.registerCustomEditor(Integer.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                if (text == null || text.trim().isEmpty()) {
                    setValue(null);
                    return;
                }
                String cleaned = text.replaceAll("[^0-9\\-]", "");
                if (cleaned.isEmpty()) {
                    setValue(null);
                    return;
                }
                setValue(Integer.valueOf(cleaned));
            }
        });

        binder.registerCustomEditor(Long.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                if (text == null || text.trim().isEmpty()) {
                    setValue(null);
                    return;
                }
                String cleaned = text.replaceAll("[^0-9\\-]", "");
                if (cleaned.isEmpty()) {
                    setValue(null);
                    return;
                }
                setValue(Long.valueOf(cleaned));
            }
        });
    }

    @GetMapping
    public String products(Model model) {
        model.addAttribute("products", productService.findAll());
        return "admin/products";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("productDTO", new ProductDTO());
        model.addAttribute("categories", categoryService.findAll());
        return "admin/product-form";
    }

    @PostMapping("/add")
    public String addProduct(@ModelAttribute ProductDTO dto,
                             @RequestParam("imageFile") MultipartFile imageFile,
                             RedirectAttributes redirectAttributes) {
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                dto.setImage(storeImage(imageFile));
            }
            productService.save(dto);
            redirectAttributes.addFlashAttribute("success", "Thêm sản phẩm thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Product product = productService.findById(id).orElse(null);
        if (product != null) {
            ProductDTO dto = new ProductDTO();
            dto.setId(product.getId());
            dto.setName(product.getName());
            dto.setDescription(product.getDescription());
            dto.setPrice(product.getPrice());
            dto.setDiscountPrice(product.getSalePrice());
            dto.setStock(product.getStock());
            dto.setImage(product.getImageUrl());
            dto.setCategoryId(product.getCategory() != null ? product.getCategory().getId() : null);
            
            model.addAttribute("productDTO", dto);
            model.addAttribute("categories", categoryService.findAll());
            return "admin/product-form";
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/update/{id}")
    public String updateProduct(@PathVariable Long id,
                                @ModelAttribute ProductDTO dto,
                                @RequestParam("imageFile") MultipartFile imageFile,
                                RedirectAttributes redirectAttributes) {
        try {
            // Nếu user upload ảnh mới thì ghi đè, không thì giữ ảnh cũ (dto.image)
            if (imageFile != null && !imageFile.isEmpty()) {
                dto.setImage(storeImage(imageFile));
            } else if (dto.getImage() == null || dto.getImage().isBlank()) {
                // Không chọn ảnh mới và form không gửi lại URL ảnh → lấy lại ảnh hiện tại trong DB
                productService.findById(id).ifPresent(p -> dto.setImage(p.getImageUrl()));
            }
            productService.update(id, dto);
            redirectAttributes.addFlashAttribute("success", "Cập nhật sản phẩm thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Xóa sản phẩm thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    private String storeImage(MultipartFile imageFile) throws IOException {
        String original = imageFile.getOriginalFilename();
        String filenameOnly = original != null ? StringUtils.getFilename(original) : "";
        String ext = "";
        int lastDot = filenameOnly.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < filenameOnly.length() - 1) {
            ext = filenameOnly.substring(lastDot);
        }
        if (ext.isEmpty()) {
            ext = ".jpg";
        }

        Files.createDirectories(Paths.get(uploadDir));
        String newName = System.currentTimeMillis() + "-" + UUID.randomUUID() + ext;
        Path target = Paths.get(uploadDir).resolve(newName);
        imageFile.transferTo(target);

        // Use leading '/' so it works from all pages (/products/..., /admin/...)
        return "/uploads/" + newName;
    }
}

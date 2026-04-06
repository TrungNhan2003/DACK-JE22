package com.fashionshop.dto;

import com.fashionshop.entity.Voucher;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class VoucherFormDTO {

    private Long id;

    @NotBlank(message = "Mã voucher không được để trống")
    private String code;

    @NotNull(message = "Chọn loại giảm giá")
    private Voucher.DiscountType discountType;

    @NotNull(message = "Nhập giá trị giảm")
    private BigDecimal discountValue;

    private boolean active = true;

    private String description;

    // Explicit getters/setters (Lombok not working with Java 25/26)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Voucher.DiscountType getDiscountType() { return discountType; }
    public void setDiscountType(Voucher.DiscountType discountType) { this.discountType = discountType; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

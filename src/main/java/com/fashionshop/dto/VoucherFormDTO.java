package com.fashionshop.dto;

import com.fashionshop.entity.Voucher;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
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
}

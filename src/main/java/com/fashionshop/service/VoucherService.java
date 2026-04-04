package com.fashionshop.service;

import com.fashionshop.dto.VoucherPreviewResponse;

import java.math.BigDecimal;

/**
 * Áp dụng mã giảm giá ở bước thanh toán (mã do admin tạo trong hệ thống).
 */
public interface VoucherService {

    /**
     * @param voucherCode mã khách nhập (có thể null/rỗng)
     * @param subtotal    tổng tiền giỏ trước giảm
     * @return invalidCode=true nếu khách nhập mã nhưng mã không hợp lệ; discountAmount và normalizedCode khi hợp lệ
     */
    DiscountResult apply(String voucherCode, BigDecimal subtotal);

    /** Xem trước giảm giá (cho AJAX trên trang checkout). */
    VoucherPreviewResponse preview(String voucherCode, BigDecimal subtotal);

    record DiscountResult(boolean invalidCode, BigDecimal discountAmount, String normalizedCode) {
        public static DiscountResult none() {
            return new DiscountResult(false, BigDecimal.ZERO, null);
        }
    }
}
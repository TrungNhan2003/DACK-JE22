package com.fashionshop.dto;

import java.math.BigDecimal;

/**
 * JSON trả về khi xem trước mã giảm giá trên trang thanh toán.
 */
public class VoucherPreviewResponse {

    private boolean valid;
    private String message;
    private BigDecimal discountAmount;
    private BigDecimal finalTotal;

    public VoucherPreviewResponse() {
    }

    public VoucherPreviewResponse(boolean valid, String message, BigDecimal discountAmount, BigDecimal finalTotal) {
        this.valid = valid;
        this.message = message;
        this.discountAmount = discountAmount;
        this.finalTotal = finalTotal;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getFinalTotal() {
        return finalTotal;
    }

    public void setFinalTotal(BigDecimal finalTotal) {
        this.finalTotal = finalTotal;
    }
}
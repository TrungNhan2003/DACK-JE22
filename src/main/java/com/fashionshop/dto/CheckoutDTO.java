package com.fashionshop.dto;

import jakarta.validation.constraints.NotBlank;

public class CheckoutDTO {

    @NotBlank(message = "Họ và tên không được để trống")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String phone;

    @NotBlank(message = "Địa chỉ không được để trống")
    private String address;

    private String paymentMethod = "COD";

    /** Mã giảm giá (tùy chọn), ví dụ: sale4.4 */
    private String voucherCode;

    private String note;

    // Explicit getters/setters (Lombok not working with Java 25/26)
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}

package com.fashionshop.service;

import com.fashionshop.dto.VoucherPreviewResponse;
import com.fashionshop.entity.Voucher;
import com.fashionshop.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final VoucherRepository voucherRepository;

    private static BigDecimal computeDiscount(Voucher v, BigDecimal subtotal) {
        if (subtotal == null || subtotal.signum() <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;
        if (v.getDiscountType() == Voucher.DiscountType.PERCENT) {
            discount = subtotal.multiply(v.getDiscountValue())
                    .divide(ONE_HUNDRED, 0, RoundingMode.HALF_UP);
        } else {
            discount = v.getDiscountValue().setScale(0, RoundingMode.HALF_UP);
        }

        if (discount.signum() < 0) {
            discount = BigDecimal.ZERO;
        }

        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
        }

        return discount;
    }

    @Override
    @Transactional(readOnly = true)
    public DiscountResult apply(String voucherCode, BigDecimal subtotal) {
        if (subtotal == null || subtotal.signum() <= 0) {
            if (voucherCode == null || voucherCode.isBlank()) {
                return DiscountResult.none();
            }
            return new DiscountResult(true, BigDecimal.ZERO, null);
        }

        if (voucherCode == null || voucherCode.isBlank()) {
            return DiscountResult.none();
        }

        String trimmed = voucherCode.trim();
        Optional<Voucher> opt = voucherRepository.findByCodeIgnoreCaseAndActiveTrue(trimmed);
        if (opt.isEmpty()) {
            return new DiscountResult(true, BigDecimal.ZERO, null);
        }

        Voucher v = opt.get();
        BigDecimal discount = computeDiscount(v, subtotal);
        return new DiscountResult(false, discount, v.getCode());
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherPreviewResponse preview(String voucherCode, BigDecimal subtotal) {
        if (subtotal == null) {
            subtotal = BigDecimal.ZERO;
        }

        if (voucherCode == null || voucherCode.isBlank()) {
            return new VoucherPreviewResponse(true, "", BigDecimal.ZERO, subtotal);
        }

        DiscountResult r = apply(voucherCode, subtotal);
        if (r.invalidCode()) {
            return new VoucherPreviewResponse(false, "Mã không hợp lệ hoặc đã ngưng sử dụng.", null, null);
        }

        BigDecimal fin = subtotal.subtract(r.discountAmount()).max(BigDecimal.ZERO);
        return new VoucherPreviewResponse(true, "", r.discountAmount(), fin);
    }
}
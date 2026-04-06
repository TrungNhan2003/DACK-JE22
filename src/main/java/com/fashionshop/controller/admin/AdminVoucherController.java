package com.fashionshop.controller.admin;

import com.fashionshop.dto.VoucherFormDTO;
import com.fashionshop.entity.Voucher;
import com.fashionshop.repository.VoucherRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/vouchers")
public class AdminVoucherController {

    private final VoucherRepository voucherRepository;

    public AdminVoucherController(VoucherRepository voucherRepository) {
        this.voucherRepository = voucherRepository;
    }

    @GetMapping
    public String list(Model model) {
        List<Voucher> list = voucherRepository.findAllByOrderByCreatedAtDesc();
        model.addAttribute("vouchers", list);
        return "admin/vouchers";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("voucherForm", new VoucherFormDTO());
        return "admin/voucher-form";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        Voucher v = voucherRepository.findById(id).orElseThrow();
        VoucherFormDTO dto = new VoucherFormDTO();
        dto.setId(v.getId());
        dto.setCode(v.getCode());
        dto.setDiscountType(v.getDiscountType());
        dto.setDiscountValue(v.getDiscountValue());
        dto.setActive(v.getActive());
        dto.setDescription(v.getDescription());
        model.addAttribute("voucherForm", dto);
        return "admin/voucher-form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("voucherForm") VoucherFormDTO dto,
                       BindingResult result,
                       RedirectAttributes redirectAttributes) {
        String code = normalizeCode(dto.getCode());
        if (code.isEmpty()) {
            result.rejectValue("code", "code.empty", "Mã không hợp lệ");
        } else {
            Optional<Voucher> dup = voucherRepository.findByCodeIgnoreCase(code);
            if (dup.isPresent()) {
                Voucher other = dup.get();
                if (dto.getId() == null || !other.getId().equals(dto.getId())) {
                    result.rejectValue("code", "code.duplicate", "Mã này đã tồn tại");
                }
            }
        }
        if (result.hasErrors()) {
            return "admin/voucher-form";
        }

        if (dto.getId() == null) {
            Voucher v = new Voucher();
            v.setCode(code);
            v.setDiscountType(dto.getDiscountType());
            v.setDiscountValue(dto.getDiscountValue());
            v.setActive(dto.isActive());
            v.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
            voucherRepository.save(v);
            redirectAttributes.addFlashAttribute("success", "Đã tạo mã giảm giá: " + code);
        } else {
            Voucher existing = voucherRepository.findById(dto.getId()).orElseThrow();
            existing.setCode(code);
            existing.setDiscountType(dto.getDiscountType());
            existing.setDiscountValue(dto.getDiscountValue());
            existing.setActive(dto.isActive());
            existing.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
            voucherRepository.save(existing);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật mã: " + code);
        }
        return "redirect:/admin/vouchers";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        voucherRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Đã xóa voucher.");
        return "redirect:/admin/vouchers";
    }

    private static String normalizeCode(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toUpperCase();
    }
}

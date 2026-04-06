package com.fashionshop.controller;

import com.fashionshop.dto.CheckoutDTO;
import com.fashionshop.dto.VoucherPreviewResponse;
import com.fashionshop.entity.Order;
import com.fashionshop.entity.User;
import com.fashionshop.service.CartService;
import com.fashionshop.service.OrderService;
import com.fashionshop.service.VoucherService;
import com.fashionshop.service.ZaloPayService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    private static final Logger log = LoggerFactory.getLogger(CheckoutController.class);

    private final CartService cartService;
    private final OrderService orderService;
    private final VoucherService voucherService;
    private final ZaloPayService zaloPayService;

    public CheckoutController(CartService cartService, OrderService orderService, VoucherService voucherService, ZaloPayService zaloPayService) {
        this.cartService = cartService;
        this.orderService = orderService;
        this.voucherService = voucherService;
        this.zaloPayService = zaloPayService;
    }

    @GetMapping
    public String showCheckout(Model model, Authentication authentication) {
        if (cartService.getCartItems().isEmpty()) {
            return "redirect:/cart";
        }

        CheckoutDTO checkoutDTO = new CheckoutDTO();
        
        // Pre-fill user info if logged in
        if (authentication != null) {
            String email = authentication.getName();
            User user = orderService.findByEmail(email).orElse(null);
            if (user != null) {
                checkoutDTO.setFullName(user.getFullName());
                checkoutDTO.setPhone(user.getPhone() != null ? user.getPhone() : "");
                checkoutDTO.setAddress(user.getAddress() != null ? user.getAddress() : "");
            }
        }

        model.addAttribute("checkoutDTO", checkoutDTO);
        enrichCheckoutModel(model, checkoutDTO);

        return "user/checkout";
    }

    @GetMapping(value = "/api/voucher-preview", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public VoucherPreviewResponse voucherPreview(@RequestParam(required = false) String code) {
        BigDecimal subtotal = cartService.getCartTotal();
        return voucherService.preview(code, subtotal);
    }

    private void enrichCheckoutModel(Model model, CheckoutDTO dto) {
        BigDecimal cartTotal = cartService.getCartTotal();
        model.addAttribute("cartItems", cartService.getCartItems());
        model.addAttribute("cartTotal", cartTotal);
        VoucherService.DiscountResult vr = voucherService.apply(dto.getVoucherCode(), cartTotal);
        if (!vr.invalidCode() && vr.discountAmount() != null && vr.discountAmount().signum() > 0) {
            model.addAttribute("voucherDiscount", vr.discountAmount());
            model.addAttribute("finalTotal", cartTotal.subtract(vr.discountAmount()).max(BigDecimal.ZERO));
        } else {
            model.addAttribute("finalTotal", cartTotal);
        }
    }

    @PostMapping
    public String processCheckout(@Valid @ModelAttribute("checkoutDTO") CheckoutDTO dto,
                                  BindingResult result, Model model, Authentication authentication) {
        BigDecimal cartTotal = cartService.getCartTotal();
        VoucherService.DiscountResult vr = voucherService.apply(dto.getVoucherCode(), cartTotal);
        if (vr.invalidCode()) {
            result.rejectValue("voucherCode", "voucher.invalid", "Mã voucher không hợp lệ");
        }

        if (result.hasErrors()) {
            enrichCheckoutModel(model, dto);
            return "user/checkout";
        }

        if (cartService.getCartItems().isEmpty()) {
            return "redirect:/cart";
        }

        String email = authentication != null ? authentication.getName() : null;
        Order order;
        try {
            order = orderService.createOrder(dto, email);
        } catch (IllegalArgumentException ex) {
            if ("invalid_voucher".equals(ex.getMessage())) {
                result.rejectValue("voucherCode", "voucher.invalid", "Mã voucher không hợp lệ");
                enrichCheckoutModel(model, dto);
                return "user/checkout";
            }
            throw ex;
        }

        if (order == null) {
            return "redirect:/checkout?error";
        }

        cartService.clearCart();

        // Handle ZaloPay payment → show QR page
        if ("MOMO".equals(dto.getPaymentMethod())) {
            try {
                Map<String, Object> zaloPayResponse = zaloPayService.createPayment(order);
                String orderUrl = (String) zaloPayResponse.get("orderUrl");
                
                log.info("📋 ZaloPay orderUrl: {}", orderUrl);
                
                if (orderUrl == null || orderUrl.isEmpty()) {
                    log.error("❌ ZaloPay returned null orderUrl");
                    throw new RuntimeException("ZaloPay không trả về orderUrl");
                }
                
                String qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data="
                        + java.net.URLEncoder.encode(orderUrl, java.nio.charset.StandardCharsets.UTF_8);

                log.info("🖼️ QR Code URL: {}", qrCodeUrl);

                model.addAttribute("order", order);
                model.addAttribute("qrCodeUrl", qrCodeUrl);
                model.addAttribute("payUrl", orderUrl);
                model.addAttribute("paymentGateway", "ZaloPay");
                return "user/checkout-momo";
            } catch (Exception e) {
                log.error("ZaloPay payment creation failed", e);
                model.addAttribute("error", "Không thể tạo thanh toán ZaloPay. Vui lòng thử lại hoặc chọn COD.");
                return "redirect:/checkout/order-success/" + order.getId();
            }
        }

        return "redirect:/checkout/order-success/" + order.getId();
    }

    @GetMapping("/order-success/{orderId}")
    public String orderSuccess(@PathVariable Long orderId, Model model) {
        Order order = orderService.findById(orderId).orElse(null);
        if (order != null) {
            model.addAttribute("order", order);
            return "user/order-success";
        }
        return "redirect:/";
    }

    // ===================================
    // ZALOPAY RETURN (user redirected here after payment)
    // ===================================
    @GetMapping("/zalopay-return")
    public String zalopayReturn(Model model) {
        log.info("ZaloPay return endpoint hit");
        // User is redirected here from ZaloPay after payment
        // Actual status update happens via callback
        return "redirect:/order-history";
    }

    // ===================================
    // ZALOPAY CALLBACK (ZaloPay server calls this)
    // ===================================
    
    // Handle POST callback with JSON body
    @PostMapping(value = "/zalopay-callback", produces = MediaType.APPLICATION_JSON_VALUE, 
                 consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    @ResponseBody
    public Map<String, Object> zalopayCallback(@RequestBody(required = false) Map<String, Object> body,
                                                @RequestParam(required = false) Map<String, String> params,
                                                HttpServletRequest request) {
        return processZaloPayCallback(body, params, request);
    }

    // Handle GET callback (query parameters)
    @GetMapping(value = "/zalopay-callback", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> zalopayCallbackGet(@RequestParam Map<String, String> params) {
        return processZaloPayCallback(null, params, null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> processZaloPayCallback(Map<String, Object> body, 
                                                        Map<String, String> params,
                                                        HttpServletRequest request) {
        log.info("ZaloPay callback received - body: {}, params: {}", body, params);

        Map<String, String> data = new HashMap<>();
        String rawDataJson = null; // Store the raw data JSON string for MAC verification
        
        // ZaloPay sends JSON with nested structure: {data: {...}, mac: "...", type: "1"}
        if (body != null && !body.isEmpty()) {
            log.info("📦 Processing JSON body callback");
            
            // Extract MAC and type from top level
            if (body.containsKey("mac")) {
                data.put("mac", body.get("mac").toString());
            }
            if (body.containsKey("type")) {
                data.put("type", body.get("type").toString());
            }
            
            // Extract actual payment data from "data" field
            if (body.containsKey("data")) {
                Object dataObj = body.get("data");
                log.info("📦 Extracting payment data from 'data' field: {}", dataObj);
                
                if (dataObj instanceof Map) {
                    // Convert the Map back to JSON string for MAC verification
                    try {
                        rawDataJson = new ObjectMapper().writeValueAsString(dataObj);
                        log.info("📝 Raw data JSON for MAC verification: {}", rawDataJson);
                    } catch (Exception e) {
                        log.error("Failed to convert data Map to JSON string", e);
                    }
                    
                    Map<String, Object> paymentData = (Map<String, Object>) dataObj;
                    paymentData.forEach((key, value) -> {
                        data.put(key, value != null ? value.toString() : null);
                    });
                } else if (dataObj instanceof String) {
                    // Sometimes data might be a JSON string
                    rawDataJson = (String) dataObj;
                    log.info("📝 Raw data JSON string for MAC verification: {}", rawDataJson);
                    
                    try {
                        Map<String, Object> paymentData = new ObjectMapper().readValue(
                            (String) dataObj, Map.class);
                        paymentData.forEach((key, value) -> {
                            data.put(key, value != null ? value.toString() : null);
                        });
                    } catch (Exception e) {
                        log.error("Failed to parse nested data JSON string", e);
                    }
                }
            }
        } 
        // Fallback to query parameters
        else if (params != null && !params.isEmpty()) {
            log.info("📦 Processing query parameters callback");
            data.putAll(params);
        }
        // Last resort: try to read from request directly
        else if (request != null) {
            log.info("📦 Trying to read raw request parameters");
            Map<String, String[]> parameterMap = request.getParameterMap();
            parameterMap.forEach((key, values) -> {
                if (values != null && values.length > 0) {
                    data.put(key, values[0]);
                }
            });
            
            // If still empty, log the raw request for debugging
            if (data.isEmpty()) {
                log.warn("⚠️ Callback data is empty! Request method: {}, Content-Type: {}", 
                        request.getMethod(), request.getContentType());
            }
        }

        log.info("📋 Extracted callback data: {}", data);

        Map<String, Object> response = new HashMap<>();
        try {
            // Verify MAC using raw JSON string (per ZaloPay docs)
            if (rawDataJson == null || !zaloPayService.verifyCallback(rawDataJson, data.get("mac"))) {
                log.warn("ZaloPay callback MAC verification failed. rawDataJson: {}, received mac: {}", 
                        rawDataJson, data.get("mac"));
                response.put("return_code", -1);
                response.put("return_message", "MAC không hợp lệ");
                return response;
            }

            String type = data.get("type"); // 1 = success
            String embedData = data.get("embed_data");

            // Extract orderId from embed_data
            Long orderId = null;
            if (embedData != null && embedData.contains("orderId")) {
                try {
                    String orderIdStr = embedData.split("\"orderId\":\"")[1].split("\"")[0];
                    orderId = Long.parseLong(orderIdStr);
                } catch (Exception e) {
                    log.error("Failed to parse orderId from embed_data: {}", embedData, e);
                }
            }

            if ("1".equals(type) && orderId != null) {
                // Update order status to PAID
                final Long finalOrderId = orderId;
                orderService.findById(finalOrderId).ifPresent(order -> {
                    order.setStatus(Order.OrderStatus.PAID);
                    orderService.save(order);
                    log.info("✅ Order {} marked as PAID via ZaloPay callback", finalOrderId);
                });
                response.put("return_code", 1);
                response.put("return_message", "success");
            } else {
                log.warn("ZaloPay payment failed or canceled. type={}, orderId={}", type, orderId);
                if (orderId != null) {
                    final Long finalOrderId = orderId;
                    orderService.findById(finalOrderId).ifPresent(order -> {
                        order.setStatus(Order.OrderStatus.CANCELLED);
                        orderService.save(order);
                        log.info("❌ Order {} marked as CANCELLED", finalOrderId);
                    });
                }
                response.put("return_code", 0);
                response.put("return_message", "Payment failed");
            }
        } catch (Exception e) {
            log.error("ZaloPay callback processing failed", e);
            response.put("return_code", -1);
            response.put("return_message", "Internal error: " + e.getMessage());
        }
        return response;
    }

    // ===================================
    // API: Check payment status (for frontend polling)
    // ===================================
    @GetMapping(value = "/api/check-payment/{orderId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> checkPaymentStatus(@PathVariable Long orderId) {
        Map<String, Object> result = new HashMap<>();
        orderService.findById(orderId).ifPresentOrElse(order -> {
            result.put("orderId", order.getId());
            result.put("status", order.getStatus().name());
        }, () -> {
            result.put("orderId", orderId);
            result.put("status", "NOT_FOUND");
        });
        return result;
    }
}

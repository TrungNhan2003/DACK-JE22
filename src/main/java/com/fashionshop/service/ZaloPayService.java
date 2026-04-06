package com.fashionshop.service;

import com.fashionshop.entity.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class ZaloPayService {

    private static final Logger log = LoggerFactory.getLogger(ZaloPayService.class);

    @Value("${zalopay.appId}")
    private String appId;

    @Value("${zalopay.key1}")
    private String key1;

    @Value("${zalopay.key2}")
    private String key2;

    @Value("${zalopay.appUser}")
    private String appUser;

    @Value("${zalopay.createOrderUrl}")
    private String createOrderUrl;

    @Value("${zalopay.callbackUrl}")
    private String callbackUrl;

    @Value("${zalopay.redirectUrl}")
    private String redirectUrl;

    private final RestTemplate restTemplate;

    public ZaloPayService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(30000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Create ZaloPay payment order.
     * Returns: orderUrl, appTransId, zpTransId
     */
    public Map<String, Object> createPayment(Order order) throws Exception {
        String appTransId = generateOrderId();
        String amount = order.getTotalAmount().stripTrailingZeros().toPlainString();

        // Store orderId in embed_data to link callback back to order
        String embedData = "{\"redirecturl\":\"" + redirectUrl + "\",\"orderId\":\"" + order.getId() + "\"}";
        String items = "[{\"name\":\"Thanh toán Fashion Shop\",\"amount\":" +
                Long.parseLong(amount) + "}]";
        String appTime = String.valueOf(System.currentTimeMillis());

        Map<String, String> requestData = new HashMap<>();
        requestData.put("app_id", appId);
        requestData.put("app_user", appUser);
        requestData.put("app_time", appTime);
        requestData.put("amount", amount);
        requestData.put("app_trans_id", appTransId);
        requestData.put("embed_data", embedData);
        requestData.put("item", items);
        requestData.put("description", "Fashion Shop - Don hang #" + order.getId());
        requestData.put("bank_code", "zalopayapp");
        requestData.put("callback_url", callbackUrl);

        // Create MAC signature
        String signData = appId + "|" + appTransId + "|" + appUser + "|" +
                amount + "|" + appTime + "|" + embedData + "|" + items;

        String mac = signHmacSha256(signData, key1);
        requestData.put("mac", mac);

        log.info("📡 Calling ZaloPay API: {}", createOrderUrl);
        log.info("🔑 app_trans_id: {}", appTransId);
        log.info("💰 amount: {}", amount);
        log.info("🔐 MAC: {}", mac);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            requestData.forEach(form::add);

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
            ResponseEntity<Map> response = restTemplate.exchange(createOrderUrl, HttpMethod.POST, entity, Map.class);

            Map<String, Object> result = response.getBody();
            log.info("✅ ZaloPay response: {}", result);

            if (result != null && "1".equals(String.valueOf(result.get("return_code")))) {
                Map<String, Object> paymentResult = new HashMap<>();
                paymentResult.put("orderUrl", result.get("order_url"));
                paymentResult.put("zpTransId", result.get("zp_trans_id"));
                paymentResult.put("appTransId", appTransId);
                return paymentResult;
            } else {
                String errorMsg = result != null ?
                        String.valueOf(result.get("return_message")) : "Unknown error";
                log.error("❌ ZaloPay error: {}", errorMsg);
                throw new RuntimeException("ZaloPay payment failed: " + errorMsg);
            }
        } catch (RestClientException e) {
            log.error("❌ ZaloPay API unreachable: {}", e.getMessage());
            throw new RuntimeException("ZaloPay sandbox đang offline. Vui lòng thử lại sau hoặc chọn COD.", e);
        }
    }

    /**
     * Verify ZaloPay callback MAC signature.
     * According to ZaloPay docs: MAC = HMAC-SHA256(entire_raw_data_json_string, key2)
     * NOT by concatenating individual fields.
     */
    public boolean verifyCallback(String rawDataJson, String receivedMac) throws Exception {
        log.info("🔐 ZaloPay MAC Verification - Raw data JSON: {}", rawDataJson);
        log.info("🔐 Received MAC: {}", receivedMac);
        
        // Compute MAC over the entire raw data JSON string
        String computedMac = signHmacSha256(rawDataJson, key2);
        
        log.info("🔐 Computed MAC: {}", computedMac);

        boolean valid = computedMac.equals(receivedMac);
        log.info("   Result: {}", valid ? "✅ OK" : "❌ FAILED");
        return valid;
    }

    /**
     * Query order status from ZaloPay.
     */
    public Map<String, Object> queryOrderStatus(String appTransId) throws Exception {
        String signData = appId + "|" + appTransId + "|" + key1;
        String mac = signHmacSha256(signData, key1);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("app_id", appId);
        form.add("app_trans_id", appTransId);
        form.add("mac", mac);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                "https://sb-openapi.zalopay.vn/v2/query", HttpMethod.POST, entity, Map.class);

        Map<String, Object> result = response.getBody();
        log.info("📋 ZaloPay query response: {}", result);
        return result;
    }

    /**
     * Generate ZaloPay order ID: yyMMdd_hhmmss_appId_random6Digits
     */
    private String generateOrderId() {
        String now = java.time.LocalDateTime.now().toString();
        String date = now.substring(2, 10).replace("-", "");
        String time = now.substring(11, 19).replace(":", "");
        int random = 100000 + (int) (Math.random() * 900000);
        return date + "_" + time + "_" + appId + "_" + random;
    }

    private String signHmacSha256(String data, String key) throws Exception {
        Mac hmacSHA256 = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmacSHA256.init(secretKeySpec);
        byte[] bytes = hmacSHA256.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}

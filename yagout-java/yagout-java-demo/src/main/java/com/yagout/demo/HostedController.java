package com.yagout.demo;

import com.yagout.sdk.build.HostedRequestBuilder;
import com.yagout.sdk.crypto.CryptoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

@Controller
public class HostedController {
    private static final Logger log = LoggerFactory.getLogger(HostedController.class);

    @Value("${gateway.url}")
    private String gatewayUrl;

    @Value("${api.url}")
    private String apiUrl;

    @Value("${me.id}")
    private String meId;

    @Value("${merchant.key.base64}")
    private String merchantKeyBase64;

    @Value("${aggregator.id:yagout}")
    private String agId;

    @Value("${base.url:http://localhost:8080}")
    private String baseUrl;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("orderNo", "ORD-" + System.currentTimeMillis());
        return "index";
    }

    @GetMapping("/pay")
    public String payGetRedirect() {
        return "redirect:/";
    }

    @PostMapping("/pay")
    public String pay(@RequestParam("order_no") String orderNo,
                      @RequestParam("amount") String amount,
                      @RequestParam(value = "email_id", required = false, defaultValue = "") String email,
                      @RequestParam(value = "mobile_no", required = false, defaultValue = "") String mobile,
                      Model model) {

        // Normalize like Node: remove insignificant trailing zeros (e.g., 1.00 -> 1)
        String amountNorm;
        try {
            java.math.BigDecimal bd = new java.math.BigDecimal(amount);
            amountNorm = bd.stripTrailingZeros().toPlainString();
        } catch (Exception ex) {
            amountNorm = amount; // fallback
        }

        String plain = HostedRequestBuilder.buildMerchantPlainString(
                agId, meId, orderNo, amountNorm,
                "ETH", "ETB", "SALE",
                baseUrl + "/payment/success", baseUrl + "/payment/failure", "WEB",
                "", email, mobile, "", "Y",
                "", "", "", "", "",
                "", "", "", "", "", "", "",
                "", "", "",
                "", "", "",
                "", "", "", "", ""
        );

        String merchantRequest = CryptoUtil.encryptBase64(plain, merchantKeyBase64);

        // hash input: merchantId~order_no~amount~ETH~ETB
        String hashInput = meId + "~" + orderNo + "~" + amountNorm + "~ETH~ETB";
        String sha256Hex = CryptoUtil.sha256Hex(hashInput);
        String encryptedHash = CryptoUtil.encryptBase64(sha256Hex, merchantKeyBase64);

        model.addAttribute("action", gatewayUrl);
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("me_id", meId);
        fields.put("merchant_request", merchantRequest);
        fields.put("hash", encryptedHash);
        model.addAttribute("fields", fields);
        return "autoPost";
    }

    private static class Picked {
        String field; String value;
        Picked(String f, String v) { this.field = f; this.value = v; }
    }

    private static Picked pickEncryptedField(Map<String, Object> container) {
        if (container == null) return new Picked("", "");
        if (container.get("txn_response") instanceof String s && !s.isEmpty()) return new Picked("txn_response", s);
        if (container.get("merchant_response") instanceof String s2 && !s2.isEmpty()) return new Picked("merchant_response", s2);
        if (container.get("response") instanceof String s3 && !s3.isEmpty()) return new Picked("response", s3);
        if (container.get("data") instanceof String s4 && !s4.isEmpty()) return new Picked("data", s4);
        for (Map.Entry<String, Object> e : container.entrySet()) {
            if (e.getValue() instanceof String s5 && !s5.isEmpty()) return new Picked(e.getKey(), s5);
        }
        return new Picked("", "");
    }

    private static Map<String, Object> parseDecryptedPayload(String str) {
        Map<String, Object> details = new LinkedHashMap<>();
        if (str == null || str.isEmpty()) return details;
    String[] pipeParts = str.split("\\|");
        if (pipeParts.length >= 13) {
            details.put("order_no", pipeParts[2]);
            details.put("amount", pipeParts[3]);
            details.put("date_time", (pipeParts[6] + " " + pipeParts[7]).trim());
            details.put("transaction_id", pipeParts[8]);
            details.put("status", pipeParts[10]);
            return details;
        }
        return details;
    }

    private static String formatPayloadOrdered(Map<String, Object> body) {
        if (body == null || body.isEmpty()) return "{}";
        String[] order = new String[]{
                "txn_response", "pg_details", "txn_details", "other_details",
                "fraud_details", "card_details", "cust_details", "bill_details", "ship_details"
        };
        List<String> keys = new ArrayList<>();
        for (String k : order) if (body.containsKey(k)) keys.add(k);
        for (String k : body.keySet()) if (!keys.contains(k)) keys.add(k);
        StringBuilder sb = new StringBuilder();
        sb.append("{"); sb.append("\n");
        for (int i = 0; i < keys.size(); i++) {
            String k = keys.get(i);
            Object v = body.get(k);
            sb.append("  ").append(k).append(": '").append(String.valueOf(v)).append("'");
            if (i < keys.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("}");
        return sb.toString();
    }

    @PostMapping("/payment/success")
    public String success(@RequestParam Map<String, Object> body, Model model) {
        Picked picked = pickEncryptedField(body);
        String decrypted;
        try { decrypted = picked.value.isEmpty() ? "" : CryptoUtil.decryptBase64(picked.value, merchantKeyBase64); }
        catch (Exception e) { decrypted = "Decryption error: " + e.getMessage(); }
        Map<String, Object> details = parseDecryptedPayload(decrypted);

    // Backend logs only (don't render decrypted to users)
    log.info("SUCCESS payload: \n{}", formatPayloadOrdered(body));
    log.info("SUCCESS picked={} details={} decrypted={}", picked.field, details, decrypted);

        // User-friendly message
        StringBuilder msg = new StringBuilder("Thank you! Your payment was successful.");
        if (details.get("amount") != null) msg.append(" Amount paid: ").append(details.get("amount")).append(".");
        if (details.get("order_no") != null) msg.append(" Order No: ").append(details.get("order_no")).append(".");
        if (details.get("date_time") != null) msg.append(" Date: ").append(details.get("date_time")).append(".");
        if (details.get("status") != null && !String.valueOf(details.get("status")).equalsIgnoreCase("successful")) {
            msg.append(" Status: ").append(details.get("status")).append(".");
        }

        model.addAttribute("pickedField", picked.field);
        model.addAttribute("details", details);
        model.addAttribute("userMessage", msg.toString());
        model.addAttribute("hint", body.isEmpty() ? "\n\nHint: Ensure form posts include merchant_response." : "");
        return "success";
    }

    @PostMapping("/payment/failure")
    public String failure(@RequestParam Map<String, Object> body, Model model) {
        Picked picked = pickEncryptedField(body);
        String decrypted;
        try { decrypted = picked.value.isEmpty() ? "" : CryptoUtil.decryptBase64(picked.value, merchantKeyBase64); }
        catch (Exception e) { decrypted = "Decryption error: " + e.getMessage(); }
        Map<String, Object> details = parseDecryptedPayload(decrypted);

    // Backend logs only (don't render decrypted to users)
    log.info("FAILURE payload: \n{}", formatPayloadOrdered(body));
    log.info("FAILURE picked={} details={} decrypted={}", picked.field, details, decrypted);

        // User-friendly message
        StringBuilder msg = new StringBuilder("Sorry, your payment could not be processed.");
        if (details.get("amount") != null) msg.append(" Amount: ").append(details.get("amount")).append(".");
        if (details.get("order_no") != null) msg.append(" Order No: ").append(details.get("order_no")).append(".");
        if (details.get("date_time") != null) msg.append(" Date: ").append(details.get("date_time")).append(".");
        if (details.get("status") != null) msg.append(" Status: ").append(details.get("status")).append(".");

        model.addAttribute("pickedField", picked.field);
        model.addAttribute("details", details);
        model.addAttribute("userMessage", msg.toString());
        model.addAttribute("hint", body.isEmpty() ? "\n\nHint: Ensure form posts include merchant_response." : "");
        return "failure";
    }

    @GetMapping("/payment/success")
    public String successGet(@RequestParam Map<String, Object> query, Model model) {
        Picked picked = pickEncryptedField(query);
        String decrypted;
        try { decrypted = picked.value.isEmpty() ? "" : CryptoUtil.decryptBase64(picked.value, merchantKeyBase64); }
        catch (Exception e) { decrypted = "Decryption error: " + e.getMessage(); }
        Map<String, Object> details = parseDecryptedPayload(decrypted);
        model.addAttribute("pickedField", picked.field);
        model.addAttribute("headers", Map.of());
        model.addAttribute("body", query);
        model.addAttribute("decrypted", decrypted);
        model.addAttribute("details", details);
        model.addAttribute("hint", "");
        return "success";
    }

    @GetMapping("/payment/failure")
    public String failureGet(@RequestParam Map<String, Object> query, Model model) {
        Picked picked = pickEncryptedField(query);
        String decrypted;
        try { decrypted = picked.value.isEmpty() ? "" : CryptoUtil.decryptBase64(picked.value, merchantKeyBase64); }
        catch (Exception e) { decrypted = "Decryption error: " + e.getMessage(); }
        Map<String, Object> details = parseDecryptedPayload(decrypted);
        model.addAttribute("pickedField", picked.field);
        model.addAttribute("headers", Map.of());
        model.addAttribute("body", query);
        model.addAttribute("decrypted", decrypted);
        model.addAttribute("details", details);
        model.addAttribute("hint", "");
        return "failure";
    }
}
